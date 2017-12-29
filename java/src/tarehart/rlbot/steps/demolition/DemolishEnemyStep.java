package tarehart.rlbot.steps.demolition;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.carpredict.CarInterceptPlanner;
import tarehart.rlbot.carpredict.CarPath;
import tarehart.rlbot.carpredict.CarPredictor;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.time.Duration;

import java.awt.*;
import java.util.Optional;

public class DemolishEnemyStep implements Step {

    private boolean enemyHadWheelContact;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        Optional<CarData> enemyCarOption = input.getEnemyCarData();

        CarData car = input.getMyCarData();
        if (!enemyCarOption.isPresent() || enemyCarOption.map(ec -> ec.isDemolished).orElse(false) || (car.boost == 0 && !car.isSupersonic)) {
            return Optional.empty();
        }

        CarData enemyCar = enemyCarOption.get();

        CarPath path = CarPredictor.predictCarMotion(enemyCar, Duration.ofSeconds(4));

        DistancePlot distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost);

        Optional<SpaceTime> carInterceptOption = CarInterceptPlanner.getCarIntercept(car, path, distancePlot);

        if (carInterceptOption.isPresent()) {
            SpaceTime carIntercept = carInterceptOption.get();

            // TODO: deal with cases where car is driving toward arena boundary

//        if (ArenaModel.isInBoundsBall(path.getSlices().get(path.getSlices().size() - 1).space)) {
//            BotLog.println("Whoops", car.playerIndex);
//        }

            AgentOutput steering = SteerUtil.steerTowardGroundPosition(car, carIntercept.space);

            double secondsTillContact = Duration.between(car.time, carIntercept.time).getSeconds();

            if (secondsTillContact < .5 && !enemyCar.hasWheelContact && (enemyHadWheelContact || enemyCar.position.z - car.position.z > 1)) {
                steering.withJump();
                if (!car.hasWheelContact) {
                    steering.withSteer(0); // Avoid dodging accidentally.
                }
            }

            enemyHadWheelContact = enemyCar.hasWheelContact;

            return Optional.of(steering);
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, enemyCar.position));
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public String getSituation() {
        return "Demolishing enemy";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
