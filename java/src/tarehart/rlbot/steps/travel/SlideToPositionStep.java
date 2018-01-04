package tarehart.rlbot.steps.travel;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.routing.PositionFacing;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Optional;
import java.util.function.Function;

public class SlideToPositionStep implements Step {

    private Function<AgentInput, Optional<PositionFacing>> targetFunction;

    private static final int AIM_AT_TARGET = 0;
    private static final int TRAVEL = 1;
    private static final int SLIDE_SPIN = 2;

    private int phase = AIM_AT_TARGET;

    private Integer turnDirection = null;
    private PositionFacing target;

    private Plan plan;

    public SlideToPositionStep(Function<AgentInput, Optional<PositionFacing>> targetFunction) {
        this.targetFunction = targetFunction;
    }


    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (Plan.activePlan(plan).isPresent()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        Optional<PositionFacing> targetOption = targetFunction.apply(input);
        if (!targetOption.isPresent()) {
            return Optional.empty();
        }
        target = targetOption.get();

        CarData car = input.getMyCarData();

        Vector2 toTarget = target.getPosition().minus(car.getPosition().flatten());

        if (phase == AIM_AT_TARGET) {

            if (toTarget.magnitude() < 10) {
                return Optional.empty();
            }


            double angle = Vector2.Companion.angle(toTarget, car.getOrientation().getNoseVector().flatten());
            if (angle < Math.PI / 12) {
                phase = TRAVEL;
            } else {
                return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.getBoostData(), target.getPosition()));
            }
        }

        if (phase == TRAVEL) {

            double distance = toTarget.magnitude();
            double slideDistance = getSlideDistance(car.getVelocity().magnitude());

            if (distance < slideDistance) {
                phase = SLIDE_SPIN;
            } else {

                double correctionRadians = toTarget.correctionAngle(target.getFacing());

                Vector2 offsetVector = VectorUtil.INSTANCE
                        .rotateVector(toTarget, -Math.signum(correctionRadians) * Math.PI / 2)
                        .scaledToMagnitude(10);

                Vector2 waypoint = target.getPosition().plus(offsetVector);

                Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, waypoint);
                if (sensibleFlip.isPresent()) {
                    this.plan = sensibleFlip.get();
                    return this.plan.getOutput(input);
                }

                return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.getBoostData(), waypoint).withBoost(car.getBoost() > 50));
            }
        }

        if (phase == SLIDE_SPIN) {
            if (turnDirection == null) {
                turnDirection = (int) Math.signum(car.getOrientation().getNoseVector().flatten().correctionAngle(toTarget));
            }

            double correctionRadians = car.getOrientation().getNoseVector().flatten().correctionAngle(target.getFacing());
            double futureRadians = correctionRadians + car.getSpin().getYawRate() * .3;

            if (futureRadians * turnDirection < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                return Optional.empty(); // Done orienting.
            }

            return Optional.of(new AgentOutput().withAcceleration(1).withSteer(-turnDirection).withSlide());
        }

        return Optional.empty(); // Something went wrong
    }

    private double getSlideDistance(double speed) {
        return speed * .8;
    }

    @Override
    public String getSituation() {
        return "Sliding to position";
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        if (target != null) {
                graphics.setColor(new Color(75, 214, 214));
                graphics.setStroke(new BasicStroke(1));
                Vector2 position = target.getPosition();
                int crossSize = 2;
                graphics.draw(new Line2D.Double(position.getX() - crossSize, position.getY() - crossSize, position.getX() + crossSize, position.getY() + crossSize));
                graphics.draw(new Line2D.Double(position.getX() - crossSize, position.getY() + crossSize, position.getX() + crossSize, position.getY() - crossSize));
        }
    }
}
