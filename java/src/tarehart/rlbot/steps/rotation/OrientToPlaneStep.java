package tarehart.rlbot.steps.rotation;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;


public abstract class OrientToPlaneStep implements Step {
    public static final double SPIN_DECELERATION = 6; // Radians per second per second
    private Function<AgentInput, Vector3> planeNormalFn;
    protected Vector3 planeNormal;
    protected boolean allowUpsideDown;
    protected boolean timeToDecelerate;
    private Double originalCorrection = null;

    public OrientToPlaneStep(Vector3 planeNormal) {
        this(planeNormal, false);
    }

    public OrientToPlaneStep(Function<AgentInput, Vector3> planeNormalFn, boolean allowUpsideDown) {
        this.planeNormalFn = planeNormalFn;
        this.allowUpsideDown = allowUpsideDown;
    }

    public OrientToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        this(input -> planeNormal, allowUpsideDown);
    }

    private double getRadiansSpentDecelerating(double angularVelocity) {
        double velocityMagnitude = Math.abs(angularVelocity);
        double spinDeceleration = getSpinDeceleration();
        double timeDecelerating = velocityMagnitude / spinDeceleration;
        return velocityMagnitude * timeDecelerating - .5 * spinDeceleration * timeDecelerating * timeDecelerating;
    }

    /**
     * This does not consider direction. You should only call it if you are already rotating toward your target.
     */
    protected boolean timeToDecelerate(double angularVelocity, double radiansRemaining) {
        return getRadiansSpentDecelerating(angularVelocity) >= Math.abs(radiansRemaining);
    }

    /**
     * Does not care if we go the "wrong way" and end up upside down.
     */
    protected double getMinimalCorrectionRadiansToPlane(Vector3 vectorNeedingCorrection, Vector3 axisOfRotation) {
        // We want vectorNeedingCorrection to be resting on the plane. If it's lined up with the planeNormal, then it's
        // doing a very poor job of that.
        Vector3 planeError = VectorUtil.INSTANCE.project(vectorNeedingCorrection, planeNormal);

        double distanceAbovePlane = planeError.magnitude() * Math.signum(planeError.dotProduct(planeNormal));

        double maxOrbitHeightAbovePlane = RotationUtil.maxOrbitHeightAbovePlane(axisOfRotation, planeNormal);
        return -Math.asin(distanceAbovePlane / maxOrbitHeightAbovePlane);
    }

    protected abstract double getOrientationCorrection(CarData car);

    protected abstract double getAngularVelocity(CarData car);

    protected abstract AgentOutput accelerate(boolean positiveRadians);

    protected abstract double getSpinDeceleration();

    private AgentOutput accelerateTowardPlane(CarData car) {

        double correctionRadians = getOrientationCorrection(car);

        double angularVelocity = getAngularVelocity(car);

        if (angularVelocity * correctionRadians > 0) {
            // We're trending toward the plane, that's good.
            if (timeToDecelerate(angularVelocity, correctionRadians)) {
                timeToDecelerate = true;
            }
        }

        return accelerate(correctionRadians > 0);
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        planeNormal = planeNormalFn.apply(input);

        if (originalCorrection == null) {
            originalCorrection = getOrientationCorrection(car);
        }

        AgentOutput output = null;
        if (!timeToDecelerate) {
            output = accelerateTowardPlane(input.getMyCarData());
        }

        // The value of timeToDecelerate can get changed by accelerateTowardPlane.
        if (timeToDecelerate) {
            if (getAngularVelocity(car) * originalCorrection < 0) {
                // We're done decelerating
                return Optional.empty();
            }

            output = accelerate(originalCorrection < 0);
        }

        output.withAcceleration(1); // Just in case we're stuck on our side on the ground

        return Optional.ofNullable(output);
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
