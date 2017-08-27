package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;

public class RollToPlaneStep extends OrientToPlaneStep {

    public RollToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public RollToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected Plan makeOrientationPlan(CarRotation current) {

        double radians = getCorrectionRadians(current.sideVector, current.noseVector);

        if (!allowUpsideDown && current.roofVector.dotProduct(planeNormal) < 0) {
            radians += Math.PI;
        }
        radians = RotationUtil.shortWay(radians);

        System.out.println("Rolling " + radians);

        return new Plan()
                .withStep(new BlindStep(new AgentOutput().withSteer(Math.signum(radians)).withSlide(), RotationUtil.getStartingImpulse(radians)))
                .withStep(new BlindStep(new AgentOutput().withSteer(-Math.signum(radians)).withSlide(), RotationUtil.getHaltingImpulse(radians)));
    }
}