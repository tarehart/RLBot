package tarehart.rlbot.carpredict

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

object CarPredictor {

    private val TIME_STEP = 0.1

    fun predictCarMotion(car: CarData, duration: Duration): CarPath {

        val velocity = Vector3(
                car.velocity.x,
                car.velocity.y,
                if (car.hasWheelContact && Math.abs(car.velocity.z) < .2) 0.0 else car.velocity.z)

        val initialSlice = CarSlice(car.position, car.time, velocity, car.orientation)

        val carPath = CarPath()
        carPath.addSlice(initialSlice)

        var secondsSoFar = 0.0

        val secondsToSimulate = duration.seconds

        var currentSlice = initialSlice

        while (secondsSoFar < secondsToSimulate) {

            var nextVel = currentSlice.velocity
            val space = currentSlice.space.plus(nextVel.scaled(TIME_STEP))

            if (currentSlice.space.z > ManeuverMath.BASE_CAR_Z) {
                nextVel = Vector3(nextVel.x, nextVel.y, nextVel.z - ArenaModel.GRAVITY * TIME_STEP)
            } else {
                nextVel = Vector3(nextVel.x, nextVel.y, 0.0)
            }

            val nextSlice = CarSlice(space, car.time.plusSeconds(secondsSoFar), nextVel, currentSlice.orientation)
            carPath.addSlice(nextSlice)

            secondsSoFar += TIME_STEP
            currentSlice = nextSlice
        }

        return carPath
    }

}
