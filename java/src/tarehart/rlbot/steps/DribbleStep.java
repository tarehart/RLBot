package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.util.Optional;

import static tarehart.rlbot.planning.GoalUtil.getEnemyGoal;
import static tarehart.rlbot.tuning.BotLog.println;

public class DribbleStep implements Step {
    public static final double DRIBBLE_DISTANCE = 20;
    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            if (plan != null && !plan.isComplete()) {
                Optional<AgentOutput> output = plan.getOutput(input);
                if (output.isPresent()) {
                    return output;
                }
            }
        }

        CarData car = input.getMyCarData();

        if (!canDribble(input, true)) {
            return Optional.empty();
        }

        Vector2 myPositonFlat = car.getPosition().flatten();
        Vector2 myDirectionFlat = car.getOrientation().getNoseVector().flatten();
        Vector2 ballPositionFlat = input.getBallPosition().flatten();
        Vector2 ballVelocityFlat = input.getBallVelocity().flatten();
        Vector2 toBallFlat = ballPositionFlat.minus(myPositonFlat);
        double flatDistance = toBallFlat.magnitude();

        double ballSpeed = ballVelocityFlat.magnitude();
        double leadSeconds = .2;

        BallPath ballPath = ArenaModel.predictBallPath(input);

        Optional<BallSlice> motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1);
        if (motionAfterWallBounce.isPresent() && Duration.Companion.between(input.getTime(), motionAfterWallBounce.get().getTime()).getSeconds() < 1) {
            return Optional.empty(); // The dribble step is not in the business of wall reads.
        }

        Vector2 futureBallPosition;
        BallSlice ballFuture = ballPath.getMotionAt(input.getTime().plusSeconds(leadSeconds)).get();
        futureBallPosition = ballFuture.getSpace().flatten();


        Vector2 scoreLocation = getEnemyGoal(input.getTeam()).getNearestEntrance(input.getBallPosition(), 3).flatten();

        Vector2 ballToGoal = scoreLocation.minus(futureBallPosition);
        Vector2 pushDirection;
        Vector2 pressurePoint;
        double approachDistance = 0;

        if (ballSpeed > 20) {
            double velocityCorrectionAngle = ballVelocityFlat.correctionAngle(ballToGoal);
            double angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * ballSpeed / 10));
            pushDirection = VectorUtil.INSTANCE.rotateVector(ballToGoal, angleTweak).normalized();
            approachDistance = VectorUtil.INSTANCE.project(toBallFlat, new Vector2(pushDirection.getY(), -pushDirection.getX())).magnitude() * 1.6 + .8;
            approachDistance = Math.min(approachDistance, 4);
            pressurePoint = futureBallPosition.minus(pushDirection.normalized().scaled(approachDistance));
        } else {
            pushDirection = ballToGoal.normalized();
            pressurePoint = futureBallPosition.minus(pushDirection);
        }


        Vector2 carToPressurePoint = pressurePoint.minus(myPositonFlat);
        Vector2 carToBall = futureBallPosition.minus(myPositonFlat);

        GameTime hurryUp = input.getTime().plusSeconds(leadSeconds);

        boolean hasLineOfSight = pushDirection.normalized().dotProduct(carToBall.normalized()) > -.2 || input.getBallPosition().getZ() > 2;
        if (!hasLineOfSight) {
            // Steer toward a farther-back waypoint.
            Vector2 fallBack = VectorUtil.INSTANCE.orthogonal(pushDirection, v -> v.dotProduct(ballToGoal) < 0).scaledToMagnitude(5);

            return Optional.of(SteerUtil.getThereOnTime(car, new SpaceTime(new Vector3(fallBack.getX(), fallBack.getY(), 0), hurryUp)));
        }

        AgentOutput dribble = SteerUtil.getThereOnTime(car, new SpaceTime(new Vector3(pressurePoint.getX(), pressurePoint.getY(), 0), hurryUp));
        if (carToPressurePoint.normalized().dotProduct(ballToGoal.normalized()) > .80 &&
                flatDistance > 3 && flatDistance < 5 && input.getBallPosition().getZ() < 2 && approachDistance < 2
                && Vector2.Companion.angle(myDirectionFlat, carToPressurePoint) < Math.PI / 12) {
            if (car.getBoost() > 0) {
                dribble.withAcceleration(1).withBoost();
            } else {
                plan = SetPieces.frontFlip();
                return plan.getOutput(input);
            }
        }
        return Optional.of(dribble);
    }

    public static boolean canDribble(AgentInput input, boolean log) {

        CarData car = input.getMyCarData();
        Vector3 ballToMe = car.getPosition().minus(input.getBallPosition());

        if (ballToMe.magnitude() > DRIBBLE_DISTANCE) {
            // It got away from us
            if (log) {
                println("Too far to dribble", input.getPlayerIndex());
            }
            return false;
        }

        if (input.getBallPosition().minus(car.getPosition()).normaliseCopy().dotProduct(
                GoalUtil.getOwnGoal(input.getTeam()).getCenter().minus(input.getBallPosition()).normaliseCopy()) > .9) {
            // Wrong side of ball
            if (log) {
                println("Wrong side of ball for dribble", input.getPlayerIndex());
            }
            return false;
        }

        if (VectorUtil.INSTANCE.flatDistance(car.getVelocity(), input.getBallVelocity()) > 30) {
            if (log) {
                println("Velocity too different to dribble.", input.getPlayerIndex());
            }
            return false;
        }

        if (BallPhysics.getGroundBounceEnergy(input.getBallPosition().getZ(), input.getBallVelocity().getZ()) > 50) {
            if (log) {
                println("Ball bouncing too hard to dribble", input.getPlayerIndex());
            }
            return false;
        }

        if (car.getPosition().getZ() > 5) {
            if (log) {
                println("Car too high to dribble", input.getPlayerIndex());
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return "Dribbling";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
