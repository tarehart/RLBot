package tarehart.rlbot.steps.rotation;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector3;

public class PitchToPlaneStep extends OrientToPlaneStep {

    public PitchToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public PitchToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected double getOrientationCorrection(CarData car) {
        Vector3 vectorNeedingCorrection = car.orientation.noseVector;
        Vector3 axisOfRotation = car.orientation.rightVector;
        double correction = getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection, axisOfRotation);

        boolean upsideDown = car.orientation.roofVector.dotProduct(planeNormal) < 0;

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
        return car.spin.pitchRate;
    }

    @Override
    protected AgentOutput accelerate(boolean positiveRadians) {
        return  new AgentOutput().withPitch(positiveRadians ? 1 : -1);
    }

    @Override
    protected double getSpinDeceleration() {
        return 6;
    }


    @Override
    public String getSituation() {
        return "Pitching in midair";
    }

}
