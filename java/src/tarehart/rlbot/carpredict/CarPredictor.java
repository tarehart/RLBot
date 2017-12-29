package tarehart.rlbot.carpredict;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.Duration;

public class CarPredictor {

    private static final double TIME_STEP = 0.1;

    public static CarPath predictCarMotion(CarData car, Duration duration) {

        Vector3 velocity = new Vector3(
                car.velocity.getX(),
                car.velocity.getY(),
                car.hasWheelContact && Math.abs(car.velocity.getZ()) < .2  ? 0 : car.velocity.getZ());

        CarSlice initialSlice = new CarSlice(car.position, car.time, velocity, car.orientation);

        CarPath carPath = new CarPath(initialSlice);

        double secondsSoFar = 0;

        double secondsToSimulate = duration.getSeconds();

        CarSlice currentSlice = initialSlice;

        while (secondsSoFar < secondsToSimulate) {

            Vector3 nextVel = currentSlice.velocity;
            Vector3 space = currentSlice.space.plus(nextVel.scaled(TIME_STEP));

            CarSlice nextSlice = new CarSlice(space, car.time.plusSeconds(secondsSoFar), nextVel, currentSlice.orientation);
            carPath.addSlice(nextSlice);

            secondsSoFar += TIME_STEP;
            currentSlice = nextSlice;
        }

        return carPath;
    }

}
