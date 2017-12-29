package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.TapStep;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.ui.ArenaDisplay;

import java.awt.*;
import java.util.Optional;

import static java.lang.Math.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static tarehart.rlbot.math.VectorUtil.rotateVector;
import static tarehart.rlbot.physics.ArenaModel.predictBallPath;
import static tarehart.rlbot.intercept.InterceptCalculator.getAerialIntercept;
import static tarehart.rlbot.planning.SteerUtil.getCorrectionAngleRad;
import static tarehart.rlbot.planning.WaypointTelemetry.set;
import static tarehart.rlbot.tuning.BotLog.println;

public class MidairStrikeStep implements Step {
    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 4;
    public static final int DODGE_TIME = 400;
    public static final double DODGE_DISTANCE = 6;
    public static final Duration MAX_TIME_FOR_AIR_DODGE = Duration.ofMillis(1500);
    public static final double UPWARD_VELOCITY_MAINTENANCE_ANGLE = .25;
    public static final double YAW_OVERCORRECT = .1;
    public static final double PITCH_OVERCORRECT = .1;
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
            lastMomentForDodge = input.time.plus(MAX_TIME_FOR_AIR_DODGE).minus(timeInAirAtStart);
            beginningOfStep = input.time;
        }

        BallPath ballPath = predictBallPath(input);
        CarData car = input.getMyCarData();
        Vector3 offset = GoalUtil.getOwnGoal(car.team).getCenter().scaledToMagnitude(3).minus(new Vector3(0, 0, .6));
        if (intercept != null) {
            Vector3 goalToBall = intercept.space.minus(GoalUtil.getEnemyGoal(car.team).getNearestEntrance(intercept.space, 4));
            offset = goalToBall.scaledToMagnitude(2.5);
            if (goalToBall.magnitude() > 50) {
                offset = new Vector3(offset.getX(), offset.getY(), -.2);
            }
        }
        Optional<SpaceTime> interceptOpportunity = getAerialIntercept(car, ballPath, offset);
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
        set(intercept.space.flatten(), car.team);
        Vector3 carToIntercept = intercept.space.minus(car.position);
        long millisTillIntercept = Duration.between(input.time, intercept.time).toMillis();
        double distance = car.position.distance(input.ballPosition);
        println("Midair strike running... Distance: " + distance, input.playerIndex);

        double correctionAngleRad = getCorrectionAngleRad(car, intercept.space);

        if (input.time.isBefore(lastMomentForDodge) && distance < DODGE_DISTANCE) {
            // Let's flip into the ball!
            if (abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                println("Front flip strike", input.playerIndex);
                plan = new Plan()
                        .withStep(new BlindStep(new AgentOutput(), Duration.ofMillis(5)))
                        .withStep(new BlindStep(new AgentOutput().withPitch(-1).withJump(), Duration.ofMillis(5)));
                return plan.getOutput(input);
            } else {
                // Dodge to the side
                println("Side flip strike", input.playerIndex);
                plan = new Plan()
                        .withStep(new BlindStep(new AgentOutput(), Duration.ofMillis(5)))
                        .withStep(new BlindStep(new AgentOutput().withSteer(correctionAngleRad < 0 ? 1 : -1).withJump(), Duration.ofMillis(5)));
                return plan.getOutput(input);
            }
        }

        double rightDirection = carToIntercept.normaliseCopy().dotProduct(car.velocity.normaliseCopy());
        double secondsSoFar = Duration.between(beginningOfStep, input.time).getSeconds();

        if (millisTillIntercept > DODGE_TIME && secondsSoFar > 2 && rightDirection < .6 || rightDirection < 0) {
            println("Failed aerial on bad angle", input.playerIndex);
            return empty();
        }

        double desiredVerticalAngle = getDesiredVerticalAngle(car.velocity, carToIntercept);

        Vector2 flatToIntercept = carToIntercept.flatten();

        Vector2 currentFlatVelocity = car.velocity.flatten();

        double leftRightCorrectionAngle = currentFlatVelocity.correctionAngle(flatToIntercept);
        Vector2 desiredFlatOrientation = rotateVector(currentFlatVelocity, leftRightCorrectionAngle + signum(leftRightCorrectionAngle) * YAW_OVERCORRECT).normalized();


        Vector3 desiredNoseVector = convertToVector3WithPitch(desiredFlatOrientation, sin(desiredVerticalAngle));

        Vector3 pitchPlaneNormal = car.orientation.rightVector.crossProduct(desiredNoseVector);
        Vector3 yawPlaneNormal = VectorUtil.rotateVector(desiredFlatOrientation, -Math.PI / 2).toVector3().normaliseCopy();

        Optional<AgentOutput> pitchOutput = new PitchToPlaneStep(pitchPlaneNormal).getOutput(input);
        Optional<AgentOutput> yawOutput = new YawToPlaneStep(yawPlaneNormal, false).getOutput(input);

        return of(mergeOrientationOutputs(pitchOutput, yawOutput).withBoost().withJump());
    }

    public static double getDesiredVerticalAngle(Vector3 velocity, Vector3 carToIntercept) {
        Vector3 idealDirection = carToIntercept.normaliseCopy();
        Vector3 currentMotion = velocity.normaliseCopy();

        Vector2 sidescrollerCurrentVelocity = getPitchVector(currentMotion);
        Vector2 sidescrollerIdealVelocity = getPitchVector(idealDirection);

        double currentVelocityAngle = new Vector2(1, 0).correctionAngle(sidescrollerCurrentVelocity);
        double idealVelocityAngle = new Vector2(1, 0).correctionAngle(sidescrollerIdealVelocity);

        double desiredVerticalAngle = idealVelocityAngle + UPWARD_VELOCITY_MAINTENANCE_ANGLE + (idealVelocityAngle - currentVelocityAngle) * PITCH_OVERCORRECT;
        desiredVerticalAngle = min(desiredVerticalAngle, PI / 2);
        return desiredVerticalAngle;
    }

    private AgentOutput mergeOrientationOutputs(Optional<AgentOutput> pitchOutput, Optional<AgentOutput> yawOutput) {
        AgentOutput output = new AgentOutput();
        if (pitchOutput.isPresent()) {
            output.withPitch(pitchOutput.get().getPitch());
        }
        if (yawOutput.isPresent()) {
            output.withSteer(yawOutput.get().getSteer());
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
    private Vector3 convertToVector3WithPitch(Vector2 flat, double zComponent) {
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
            ArenaDisplay.drawBall(intercept.space, graphics, new Color(23, 194, 8));
        }
    }
}
