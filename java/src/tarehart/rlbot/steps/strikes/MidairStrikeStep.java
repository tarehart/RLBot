package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.AerialMath;
import tarehart.rlbot.intercept.InterceptCalculator;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.TapStep;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.ui.ArenaDisplay;

import java.awt.*;
import java.util.Optional;

import static java.lang.Math.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static tarehart.rlbot.physics.ArenaModel.predictBallPath;
import static tarehart.rlbot.planning.WaypointTelemetry.set;
import static tarehart.rlbot.tuning.BotLog.println;

public class MidairStrikeStep implements Step {
    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 4;
    private static final Duration DODGE_TIME = Duration.Companion.ofMillis(400);
    //private static final double DODGE_DISTANCE = 6;
    public static final Duration MAX_TIME_FOR_AIR_DODGE = Duration.Companion.ofSeconds(1.4);
    private static final double YAW_OVERCORRECT = .1;
    private static final Duration NOSE_FINESSE_TIME = Duration.Companion.ofMillis(800);

    private int confusionCount = 0;
    private Plan plan;
    private GameTime lastMomentForDodge;
    private GameTime beginningOfStep;
    private Duration timeInAirAtStart;
    private SpaceTime intercept;

    public MidairStrikeStep(Duration timeInAirAtStart) {
        this.timeInAirAtStart = timeInAirAtStart;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null) {
            if (plan.isComplete()) {
                return empty();
            }
            return plan.getOutput(input);
        }

        if (lastMomentForDodge == null) {
            lastMomentForDodge = input.getTime().plus(MAX_TIME_FOR_AIR_DODGE).minus(timeInAirAtStart);
            beginningOfStep = input.getTime();
        }

        // We hold down the jump button during the aerial for extra upward acceleration, but it wears off.
        Duration timeSinceLaunch = Duration.Companion.between(beginningOfStep, input.getTime());

        BallPath ballPath = predictBallPath(input);
        CarData car = input.getMyCarData();
        Vector3 offset = GoalUtil.INSTANCE.getOwnGoal(car.getTeam()).getCenter().scaledToMagnitude(3).minus(new Vector3(0, 0, .6));
        if (intercept != null) {
            Vector3 goalToBall = intercept.getSpace().minus(GoalUtil.INSTANCE.getEnemyGoal(car.getTeam()).getNearestEntrance(intercept.getSpace(), 4));
            offset = goalToBall.scaledToMagnitude(3);
            if (goalToBall.magnitude() > 110) {
                offset = new Vector3(offset.getX(), offset.getY(), -.2);
            }
        }
        Optional<SpaceTime> interceptOpportunity = InterceptCalculator.INSTANCE.getAerialIntercept(car, ballPath, offset, beginningOfStep);
        if (!interceptOpportunity.isPresent()) {
            confusionCount++;
            if (confusionCount > 3) {
                // Front flip out of confusion
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withPitch(-1).withJump()));
                return plan.getOutput(input);
            }
            return of(new AgentOutput().withBoost());
        }
        intercept = interceptOpportunity.get();
        set(intercept.getSpace().flatten(), car.getTeam());
        Vector3 carToIntercept = intercept.getSpace().minus(car.getPosition());
        long millisTillIntercept = Duration.Companion.between(input.getTime(), intercept.getTime()).getMillis();
        double distance = car.getPosition().distance(input.getBallPosition());

        double correctionAngleRad = SteerUtil.INSTANCE.getCorrectionAngleRad(car, intercept.getSpace());

        if (input.getTime().isBefore(lastMomentForDodge) && distance < DODGE_TIME.getSeconds() * car.getVelocity().magnitude()) {
            // Let's flip into the ball!
            if (abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                println("Front flip strike", input.getPlayerIndex());
                plan = new Plan()
                        .withStep(new BlindStep(new AgentOutput(), Duration.Companion.ofMillis(5)))
                        .withStep(new BlindStep(new AgentOutput().withPitch(-1).withJump(), Duration.Companion.ofSeconds(1)));
                return plan.getOutput(input);
            } else {
                // Dodge to the side
                println("Side flip strike", input.getPlayerIndex());
                plan = new Plan()
                        .withStep(new BlindStep(new AgentOutput(), Duration.Companion.ofMillis(5)))
                        .withStep(new BlindStep(new AgentOutput().withSteer(correctionAngleRad < 0 ? 1 : -1).withJump(), Duration.Companion.ofMillis(5)));
                return plan.getOutput(input);
            }
        }

        double rightDirection = carToIntercept.normaliseCopy().dotProduct(car.getVelocity().normaliseCopy());
        double secondsSoFar = Duration.Companion.between(beginningOfStep, input.getTime()).getSeconds();

        if (millisTillIntercept > DODGE_TIME.getMillis() && secondsSoFar > 2 && rightDirection < .6) {
            println("Failed aerial on bad angle", input.getPlayerIndex());
            return empty();
        }

        double projectedHeight = AerialMath.INSTANCE.getProjectedHeight(
                car, Duration.Companion.between(car.getTime(), intercept.getTime()).getSeconds(),
                timeSinceLaunch.getSeconds()
        );

        double heightError = projectedHeight - intercept.getSpace().getZ();

        Vector2 flatToIntercept = carToIntercept.flatten();

        Vector2 currentFlatVelocity = car.getVelocity().flatten();

        double leftRightCorrectionAngle = currentFlatVelocity.correctionAngle(flatToIntercept);
        Vector2 desiredFlatOrientation = VectorUtil.INSTANCE
                .rotateVector(currentFlatVelocity, leftRightCorrectionAngle + signum(leftRightCorrectionAngle) * YAW_OVERCORRECT)
                .normalized();

        Vector3 desiredNoseVector;

        if (millisTillIntercept < NOSE_FINESSE_TIME.getMillis() && intercept.getTime().isAfter(lastMomentForDodge) && heightError > 0 && offset.getZ() > 0) {
            // Nose into the ball
            desiredNoseVector = offset.scaled(-1).normaliseCopy();
        } else {
            // Fly toward intercept
            double extraHeight = offset.getZ() > 0 ? 1.6 : 0;
            double desiredZ = AerialMath.INSTANCE.getDesiredZComponentBasedOnAccel(
                    intercept.getSpace().getZ() + extraHeight,
                    Duration.Companion.between(car.getTime(), intercept.getTime()),
                    timeSinceLaunch,
                    car);
            desiredNoseVector = convertToVector3WithPitch(desiredFlatOrientation, desiredZ);
        }


        Vector3 pitchPlaneNormal = car.getOrientation().getRightVector().crossProduct(desiredNoseVector);
        Vector3 yawPlaneNormal = VectorUtil.INSTANCE.rotateVector(desiredFlatOrientation, -Math.PI / 2).toVector3().normaliseCopy();

        Optional<AgentOutput> pitchOutput = new PitchToPlaneStep(pitchPlaneNormal).getOutput(input);
        Optional<AgentOutput> yawOutput = new YawToPlaneStep(yawPlaneNormal, false).getOutput(input);
        Optional<AgentOutput> rollOutput = new RollToPlaneStep(new Vector3(0, 0, 1), false).getOutput(input);

        return of(mergeOrientationOutputs(pitchOutput, yawOutput, rollOutput).withBoost().withJump());
    }


    private AgentOutput mergeOrientationOutputs(Optional<AgentOutput> pitchOutput, Optional<AgentOutput> yawOutput, Optional<AgentOutput> rollOutput) {
        AgentOutput output = new AgentOutput();
        if (pitchOutput.isPresent()) {
            output.withPitch(pitchOutput.get().getPitch());
        }
        if (yawOutput.isPresent()) {
            output.withSteer(yawOutput.get().getSteer());
        }
        if (rollOutput.isPresent()) {
            output.withRoll(rollOutput.get().getRoll());
        }

        return output;
    }

    /**
     * Pretend this is suddenly a 2D sidescroller where the car can't steer, it just boosts up and down.
     * Translate into that world.
     *
     * @param unitDirection normalized vector pointing in some direction
     * @return A unit vector in two dimensions, with positive x, and z equal to unitDirection z.
     */
    private static Vector2 getPitchVector(Vector3 unitDirection) {
        return new Vector2(Math.sqrt(1 - unitDirection.getZ() * unitDirection.getZ()), unitDirection.getZ());
    }

    /**
     * Return a unit vector with the given z component, and the same flat angle as flatDirection.
     */
    private static Vector3 convertToVector3WithPitch(Vector2 flat, double zComponent) {
        double xyScaler = Math.sqrt((1 - zComponent * zComponent) / (flat.getX() * flat.getX() + flat.getY() * flat.getY()));
        return new Vector3(flat.getX() * xyScaler, flat.getY() * xyScaler, zComponent);
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Finishing aerial";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        if (intercept != null) {
            ArenaDisplay.drawBall(intercept.getSpace(), graphics, new Color(23, 194, 8));
        }
    }
}
