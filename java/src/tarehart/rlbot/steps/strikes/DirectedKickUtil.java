package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.AirTouchPlanner;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.planning.StrikeProfile;
import tarehart.rlbot.tuning.ManeuverMath;

import java.time.Duration;
import java.util.Optional;

public class DirectedKickUtil {
    private static final double BALL_VELOCITY_INFLUENCE = .3;

    public static Optional<DirectedKickPlan> planKick(AgentInput input, KickStrategy kickStrategy, boolean isSideHit) {
        Vector3 interceptModifier = kickStrategy.getKickDirection(input).normaliseCopy().scaled(-2);
        return planKick(input, kickStrategy, isSideHit, interceptModifier, new StrikeProfile(.5, 0, 0));
    }

    static Optional<DirectedKickPlan> planKick(AgentInput input, KickStrategy kickStrategy, boolean isSideHit, Vector3 interceptModifier, StrikeProfile strikeProfile) {
        final DirectedKickPlan kickPlan = new DirectedKickPlan();
        kickPlan.interceptModifier = interceptModifier;

        CarData car = input.getMyCarData();

        kickPlan.ballPath = ArenaModel.predictBallPath(input);
        kickPlan.distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(car, kickPlan.ballPath, kickPlan.distancePlot, interceptModifier, AirTouchPlanner::isJumpSideFlipAccessible, strikeProfile);
        Optional<BallSlice> ballMotion = interceptOpportunity.flatMap(inter -> kickPlan.ballPath.getMotionAt(inter.time));

        if (!ballMotion.isPresent() || !interceptOpportunity.isPresent()) {
            return Optional.empty();
        }
        kickPlan.ballAtIntercept = ballMotion.get();

        double secondsTillImpactRoughly = TimeUtil.secondsBetween(input.time, kickPlan.ballAtIntercept.getTime());
        double impactSpeed = isSideHit ? ManeuverMath.DODGE_SPEED :
                kickPlan.distancePlot.getMotionAfterSeconds(secondsTillImpactRoughly).map(dts -> dts.speed).orElse(AccelerationModel.SUPERSONIC_SPEED);

        Vector3 easyForce;
        if (isSideHit) {
            Vector2 carToIntercept = interceptOpportunity.get().space.minus(car.position).flatten();
            Vector2 sideHit = VectorUtil.orthogonal(carToIntercept, v -> v.dotProduct(interceptModifier.flatten()) < 0);
            easyForce = new Vector3(sideHit.x, sideHit.y, 0);
        } else {
            easyForce = kickPlan.ballAtIntercept.getSpace().minus(car.position);
        }

        easyForce.scaledToMagnitude(impactSpeed);

        Vector3 easyKick = bump(kickPlan.ballAtIntercept.getVelocity(), easyForce);
        Vector3 kickDirection = kickStrategy.getKickDirection(input, kickPlan.ballAtIntercept.getSpace(), easyKick);

        if (easyKick.x == kickDirection.x && easyKick.y == kickDirection.y) {
            // The kick strategy is fine with the easy kick.
            kickPlan.plannedKickForce = easyForce;
            kickPlan.desiredBallVelocity = easyKick;
        } else {

            // TODO: this is a rough approximation.
            Vector2 orthogonal = VectorUtil.orthogonal(kickDirection.flatten());
            Vector2 transverseBallVelocity = VectorUtil.project(kickPlan.ballAtIntercept.getVelocity().flatten(), orthogonal);
            kickPlan.desiredBallVelocity = kickDirection.normaliseCopy().scaled(impactSpeed + transverseBallVelocity.magnitude() * .7);
            kickPlan.plannedKickForce = new Vector3(
                    kickPlan.desiredBallVelocity.x - transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE,
                    kickPlan.desiredBallVelocity.y - transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE,
                    kickPlan.desiredBallVelocity.z);
        }

        return Optional.of(kickPlan);
    }


    /**
     * https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
     */
    private static Vector3 reflect(Vector3 incident, Vector3 normal) {
        normal = normal.normaliseCopy();
        return incident.minus(normal.scaled(2 * incident.dotProduct(normal)));
    }

    private static Vector3 bump(Vector3 incident, Vector3 movingWall) {
        // Move into reference frame of moving wall
        Vector3 incidentAccordingToWall = incident.minus(movingWall);
        Vector3 reflectionAccordingToWall = reflect(incidentAccordingToWall, movingWall);
        return reflectionAccordingToWall.plus(movingWall);
    }


    static double getAngleOfKickFromApproach(CarData car, DirectedKickPlan kickPlan) {
        Vector2 strikeForceFlat = kickPlan.plannedKickForce.flatten();
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();
        Vector2 carToIntercept = carPositionAtIntercept.minus(car.position).flatten();
        return carToIntercept.correctionAngle(strikeForceFlat);
    }
}
