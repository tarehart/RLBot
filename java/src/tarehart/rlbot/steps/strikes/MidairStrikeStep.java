package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.planning.WaypointTelemetry;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.TapStep;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class MidairStrikeStep implements Step {

    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 4;
    public static final int DODGE_TIME = 400;
    public static final double DODGE_DISTANCE = 5;
    private static final Duration maxTimeForAirDodge = Duration.ofMillis(1500);
    public static final double UPWARD_VELOCITY_MAINTENANCE_ANGLE = .35;
    public static final double YAW_OVERCORRECT = .2;
    public static final double PITCH_OVERCORRECT = .3;
    private int confusionCount = 0;
    private Plan plan;
    private LocalDateTime lastMomentForDodge;
    private LocalDateTime beginningOfStep;
    private Duration timeInAirAtStart;

    public MidairStrikeStep(Duration timeInAirAtStart) {
        this.timeInAirAtStart = timeInAirAtStart;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null) {
            if (plan.isComplete()) {
                return Optional.empty();
            }
            return plan.getOutput(input);
        }

        if (lastMomentForDodge == null) {
            lastMomentForDodge = input.time.plus(maxTimeForAirDodge).minus(timeInAirAtStart);
            beginningOfStep = input.time;
        }

        BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(5));
        CarData car = input.getMyCarData();
        DistancePlot airAccelPlot = AccelerationModel.simulateAirAcceleration(car, Duration.ofSeconds(5));
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getInterceptOpportunity(car, ballPath, airAccelPlot);
        if (!interceptOpportunity.isPresent()) {
            confusionCount++;
            if (confusionCount > 3) {
                // Front flip out of confusion
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withPitch(-1).withJump()));
                plan.begin();
                return plan.getOutput(input);
            }
            return Optional.of(new AgentOutput().withBoost());
        }
        SpaceTime intercept = interceptOpportunity.get();
        WaypointTelemetry.set(intercept.space.flatten(), car.team);
        Vector3 carToIntercept = intercept.space.minus(car.position);
        long millisTillIntercept = Duration.between(input.time, intercept.time).toMillis();
        double distance = car.position.distance(input.ballPosition);
        BotLog.println("Midair strike running... Distance: " + distance, input.team);

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, intercept.space);

        if (input.time.isBefore(lastMomentForDodge) && distance < DODGE_DISTANCE) {
            // Let's flip into the ball!
            if (Math.abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD && car.velocity.normaliseCopy().z < .3) {
                BotLog.println("Front flip strike", input.team);
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withPitch(-1).withJump()));
                plan.begin();
                return plan.getOutput(input);
            } else {
                // Dodge to the side
                BotLog.println("Side flip strike", input.team);
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withSteer(correctionAngleRad < 0 ? 1 : -1).withJump()));
                plan.begin();
                return plan.getOutput(input);
            }
        }

        double rightDirection = carToIntercept.normaliseCopy().dotProduct(car.velocity.normaliseCopy());
        double secondsSoFar = TimeUtil.secondsBetween(beginningOfStep, input.time);

        if (millisTillIntercept > DODGE_TIME && secondsSoFar > 2 && rightDirection < .6 || rightDirection < 0) {
            BotLog.println("Failed aerial on bad angle", input.team);
            return Optional.empty();
        }

        Vector3 idealDirection = carToIntercept.normaliseCopy();
        Vector3 currentMotion = car.velocity.normaliseCopy();

        Vector2 sidescrollerCurrentVelocity = getPitchVector(currentMotion);
        Vector2 sidescrollerIdealVelocity = getPitchVector(idealDirection);

        double currentVelocityAngle = new Vector2(1, 0).correctionAngle(sidescrollerCurrentVelocity);
        double idealVelocityAngle = new Vector2(1, 0).correctionAngle(sidescrollerIdealVelocity);

        double desiredVerticalAngle = idealVelocityAngle + UPWARD_VELOCITY_MAINTENANCE_ANGLE + (idealVelocityAngle - currentVelocityAngle) * PITCH_OVERCORRECT;
        desiredVerticalAngle = Math.min(desiredVerticalAngle, Math.PI / 2);

        Vector2 flatToIntercept = carToIntercept.flatten();

        Vector2 currentFlatVelocity = car.velocity.flatten();

        double leftRightCorrectionAngle = currentFlatVelocity.correctionAngle(flatToIntercept);
        Vector2 desiredFlatOrientation = VectorUtil.rotateVector(currentFlatVelocity, leftRightCorrectionAngle + Math.signum(leftRightCorrectionAngle) * YAW_OVERCORRECT).normalized();


        Vector3 desiredNoseVector = convertToVector3WithPitch(desiredFlatOrientation, Math.sin(desiredVerticalAngle));

        Vector3 pitchPlaneNormal = car.orientation.rightVector.crossProduct(desiredNoseVector);
        Vector3 yawPlaneNormal = desiredNoseVector.crossProduct(new Vector3(0, 0, 1));

        Optional<AgentOutput> pitchOutput = new PitchToPlaneStep(pitchPlaneNormal).getOutput(input);
        Optional<AgentOutput> yawOutput = new YawToPlaneStep(yawPlaneNormal, false).getOutput(input);

        return Optional.of(mergeOrientationOutputs(pitchOutput, yawOutput).withBoost().withJump(millisTillIntercept > DODGE_TIME + 100));
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
    private Vector2 getPitchVector(Vector3 unitDirection) {
        return new Vector2(Math.sqrt(1 - unitDirection.z * unitDirection.z), unitDirection.z);
    }

    /**
     * Return a unit vector with the given z component, and the same flat angle as flatDirection.
     */
    private Vector3 convertToVector3WithPitch(Vector2 flat, double zComponent) {
        double xyScaler = (1 - zComponent * zComponent) / (flat.x * flat.x + flat.y * flat.y);
        return new Vector3(flat.x * xyScaler, flat.y * xyScaler, zComponent);
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Finishing aerial";
    }
}
