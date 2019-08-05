package tarehart.rlbot.carpredict

import rlbot.render.Renderer
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import java.util.*

class CarPredictor(private val carIndex: Int, private val respectFloor: Boolean = true) {


    private val SECONDS_SPENT_CIRCLING = 0.25
    private var sliceHistory = LinkedList<CarData>()

    fun predictCarMotion(bundle: TacticalBundle, duration: Duration): CarPath {
        val input = bundle.agentInput
        val car = input.allCars[carIndex]

        val carPath: CarPath

        if (sliceHistory.isEmpty() || sliceHistory.last.time != car.time) {
            sliceHistory.add(car)
        }

        if (sliceHistory.size >= 4) {
            sliceHistory.removeFirst()
        }

        if (sliceHistory.size >= 3 && car.hasWheelContact && !ArenaModel.isCarOnWall(car)) {
            val p1 = sliceHistory[0].position.flatten()
            val p2 = sliceHistory[1].position.flatten()
            val p3 = sliceHistory[2].position.flatten()

            val circle = Circle.getCircleFromPoints(p1, p2, p3)

            if (circle.radius > 500) {
                carPath = doNaivePrediction(car, duration, Vector3(), respectFloor)
            } else {
                val shortTermVector = p2 - p1
                val longTermVector = p3 - p1

                val turningLeft = shortTermVector.correctionAngle(longTermVector) > 0

                carPath = doCirclePrediction(car, circle, turningLeft, duration, car.renderer)
            }
        } else {
            carPath = doNaivePrediction(car, duration, Vector3(), respectFloor)
        }

        return carPath
    }

    private fun doCirclePrediction(car: CarData, circle: Circle, turningLeft: Boolean, duration: Duration, renderer: Renderer): CarPath {

        val radiansForTimeStep = car.velocity.flatten().magnitude() * TIME_STEP / circle.radius

        val carPath = CarPath()
        carPath.addSlice(CarSlice(car.position, car.time, car.velocity, car.orientation))

        var secondsSoFar = 0.0
        val secondsToSimulate = duration.seconds

        while (secondsSoFar < SECONDS_SPENT_CIRCLING) {

            val centerToCar = carPath.lastSlice.space.flatten() - circle.center
            val radial = VectorUtil.rotateVector(centerToCar, if (turningLeft) radiansForTimeStep else -radiansForTimeStep)
            val facing = VectorUtil.orthogonal(radial, turningLeft).toVector3()
            val nextVectorFlat = circle.center + radial
            val nextVector = Vector3(nextVectorFlat.x, nextVectorFlat.y, car.position.z)

            carPath.addSlice(CarSlice(
                    nextVector,
                    car.time.plusSeconds(secondsSoFar),
                    facing.scaledToMagnitude(car.velocity.magnitude()),
                    CarOrientation(facing.normaliseCopy(), Vector3.UP)))

            secondsSoFar += TIME_STEP
        }

        var currentSlice = carPath.lastSlice

        while (secondsSoFar < secondsToSimulate) {

            var nextVel = currentSlice.velocity

            val space = currentSlice.space.plus(nextVel.scaled(TIME_STEP))
            nextVel = Vector3(nextVel.x, nextVel.y, 0.0)

            val nextSlice = CarSlice(space, car.time.plusSeconds(secondsSoFar), nextVel, currentSlice.orientation)
            carPath.addSlice(nextSlice)

            secondsSoFar += TIME_STEP
            currentSlice = nextSlice
        }

        return carPath
    }

    companion object {
        private val TIME_STEP = 0.02

        fun doNaivePrediction(car: CarData, duration: Duration, acceleration: Vector3, respectFloor: Boolean = true): CarPath {
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
                if (!acceleration.isZero) {
                    nextVel += acceleration.scaledToMagnitude(TIME_STEP)
                }
                val space = currentSlice.space.plus(nextVel.scaled(TIME_STEP))

                if (!car.hasWheelContact) {
                    nextVel.withZ(nextVel.z - ArenaModel.GRAVITY * TIME_STEP)
                }

                if (currentSlice.space.z < ManeuverMath.BASE_CAR_Z && respectFloor) {
                    nextVel.withZ(0.0)
                }

                val nextSlice = CarSlice(space, car.time.plusSeconds(secondsSoFar), nextVel, currentSlice.orientation)
                carPath.addSlice(nextSlice)

                secondsSoFar += TIME_STEP
                currentSlice = nextSlice
            }
            return carPath
        }
    }

}
