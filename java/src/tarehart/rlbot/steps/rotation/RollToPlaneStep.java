package tarehart.rlbot.steps.rotation;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector3;

public class RollToPlaneStep extends OrientToPlaneStep {

    public RollToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public RollToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected double getOrientationCorrection(CarData car) {
        Vector3 vectorNeedingCorrection = car.getOrientation().getRightVector();
        Vector3 axisOfRotation = car.getOrientation().getNoseVector();

        // Negate the correction radians. If the right vector is above the plane, the function will indicate a negative
        // correction, but we need to roll right which is considered the positive direction.
        double correction = -getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection, axisOfRotation);

        boolean upsideDown = car.getOrientation().getRoofVector().dotProduct(planeNormal) < 0;

        if (upsideDown) {
            correction *= -1; // When upside down, need to rotate the opposite direction to converge on plane.
            if (!allowUpsideDown) {
                correction += Math.PI; // Turn all the way around
            }
        }

        return RotationUtil.shortWay(correction);
    }

    @Override
    protected double getAngularVelocity(CarData car) {
        return car.getSpin().getRollRate();
    }

    @Override
    protected AgentOutput accelerate(boolean positiveRadians) {
        return  new AgentOutput().withSteer(positiveRadians ? 1 : -1).withSlide();
    }

    @Override
    protected double getSpinDeceleration() {
        return 80;
    }

    @Override
    public String getSituation() {
        return "Rolling in midair";
    }
}
