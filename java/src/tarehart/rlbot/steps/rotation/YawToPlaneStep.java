package tarehart.rlbot.steps.rotation;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector3;

import java.util.function.Function;

public class YawToPlaneStep extends OrientToPlaneStep {

    public YawToPlaneStep(Function<AgentInput, Vector3> planeNormalFn) {
        super(planeNormalFn, false);
    }

    public YawToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected double getOrientationCorrection(CarData car) {
        Vector3 vectorNeedingCorrection = car.orientation.noseVector;
        Vector3 axisOfRotation = car.orientation.roofVector;
        double correction = getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection, axisOfRotation);

        boolean wrongDirection = car.orientation.rightVector.dotProduct(planeNormal) < 0;

        if (wrongDirection) {
            correction *= -1; // When upside down, need to rotate the opposite direction to converge on plane.
            if (!allowUpsideDown) {
                correction += Math.PI; // Turn all the way around
            }
        }
        return RotationUtil.shortWay(correction);
    }

    @Override
    protected double getAngularVelocity(CarData car) {
        return car.spin.yawRate;
    }

    @Override
    protected AgentOutput accelerate(boolean positiveRadians) {
        return  new AgentOutput().withSteer(positiveRadians ? 1 : -1);
    }

    @Override
    protected double getSpinDeceleration() {
        return 6;
    }

    @Override
    public String getSituation() {
        return "Yawing in midair";
    }
}
