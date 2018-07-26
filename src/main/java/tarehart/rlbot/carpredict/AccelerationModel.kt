package tarehart.rlbot.carpredict

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

import java.util.Optional

object AccelerationModel {

    val SUPERSONIC_SPEED = 46.0
    val MEDIUM_SPEED = 28.0
    val FLIP_THRESHOLD_SPEED = 20.0

    private val TIME_STEP = 0.1
    private val FRONT_FLIP_SPEED_BOOST = 10.0
    private val FRONT_FLIP_SECONDS = 1.5

    private val INCREMENTAL_BOOST_ACCELERATION = 19
    private val AIR_BOOST_ACCELERATION = 19.0 // It's a tiny bit faster, but account for course correction wiggle
    private val BOOST_CONSUMED_PER_SECOND = 25.0


    fun getTravelSeconds(carData: CarData, plot: DistancePlot, target: Vector3): Duration? {
        val distance = carData.position.distance(target)
        val travelTime = plot.getTravelTime(distance)
        val penaltySeconds = getSteerPenaltySeconds(carData, target)
        return travelTime?.plusSeconds(penaltySeconds)
    }

    fun getOrientDuration(carData: CarData, space: Vector3): Duration {
        return Duration.ofSeconds(getSteerPenaltySeconds(carData, space))
    }

    fun getSteerPenaltySeconds(carData: CarData, target: Vector3): Double {
        val toTarget = target.minus(carData.position)
        val correctionAngleRad = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toTarget, carData.orientation.roofVector)
        val correctionErr = Math.abs(correctionAngleRad)

        return if (correctionErr < Math.PI / 6) {
            0.0
        } else correctionErr * .1 + correctionErr * carData.velocity.magnitude() * .005
    }

    @JvmOverloads
    fun simulateAcceleration(carData: CarData, duration: Duration, boostBudget: Double, flipCutoffDistance: Double = java.lang.Double.MAX_VALUE): DistancePlot {

        var currentSpeed = ManeuverMath.forwardSpeed(carData)
        val plot = DistancePlot(DistanceTimeSpeed(0.0, Duration.ofMillis(0), currentSpeed))

        var boostRemaining = boostBudget

        var distanceSoFar = 0.0
        var secondsSoFar = 0.0

        val secondsToSimulate = duration.seconds

        while (secondsSoFar < secondsToSimulate) {
            val hypotheticalFrontFlipDistance = getFrontFlipDistance(currentSpeed)


            if (currentSpeed >= SUPERSONIC_SPEED) {
                // It gets boring from zero on. Put a slice at the very end.
                val secondsRemaining = secondsToSimulate - secondsSoFar
                plot.addSlice(DistanceTimeSpeed(distanceSoFar + SUPERSONIC_SPEED * secondsRemaining, duration, SUPERSONIC_SPEED))
                break
            } else if (boostRemaining <= 0 && currentSpeed > FLIP_THRESHOLD_SPEED && distanceSoFar + hypotheticalFrontFlipDistance < flipCutoffDistance) {
                currentSpeed += FRONT_FLIP_SPEED_BOOST
                if (currentSpeed > SUPERSONIC_SPEED) {
                    currentSpeed = SUPERSONIC_SPEED
                }
                plot.addSlice(DistanceTimeSpeed(distanceSoFar, Duration.ofSeconds(secondsSoFar), currentSpeed))
                secondsSoFar += FRONT_FLIP_SECONDS
                distanceSoFar += hypotheticalFrontFlipDistance
                plot.addSlice(DistanceTimeSpeed(distanceSoFar, Duration.ofSeconds(secondsSoFar), currentSpeed))

            } else {
                val acceleration = getAcceleration(currentSpeed, boostRemaining > 0)
                val nextSpeed = Math.min(SUPERSONIC_SPEED, currentSpeed + acceleration * TIME_STEP)
                boostRemaining -= BOOST_CONSUMED_PER_SECOND * TIME_STEP
                distanceSoFar += ((currentSpeed + nextSpeed) / 2) * TIME_STEP
                secondsSoFar += TIME_STEP
                currentSpeed = nextSpeed;
                plot.addSlice(DistanceTimeSpeed(distanceSoFar, Duration.ofSeconds(secondsSoFar), currentSpeed))
            }
        }

        return plot
    }

    private fun getAcceleration(currentSpeed: Double, hasBoost: Boolean): Double {

        if (currentSpeed >= SUPERSONIC_SPEED || !hasBoost && currentSpeed >= MEDIUM_SPEED) {
            return 0.0
        }

        var accel = 0.0
        if (currentSpeed < MEDIUM_SPEED) {
            accel = 30 - currentSpeed * .95
        }

        if (hasBoost) {
            accel += INCREMENTAL_BOOST_ACCELERATION
        }

        return accel
    }

    fun getFrontFlipDistance(speed: Double): Double {
        return (speed + FRONT_FLIP_SPEED_BOOST) * FRONT_FLIP_SECONDS
    }

    fun simulateAirAcceleration(car: CarData, duration: Duration, horizontalPitchComponent: Double): DistancePlot {
        var currentSpeed = car.velocity.flatten().magnitude()
        val plot = DistancePlot(DistanceTimeSpeed(0.0, Duration.ofMillis(0), currentSpeed))

        var boostRemaining = car.boost

        var distanceSoFar = 0.0
        var secondsSoFar = 0.0

        val secondsToSimulate = duration.seconds

        while (secondsSoFar < secondsToSimulate) {

            val acceleration = if (boostRemaining > 0) horizontalPitchComponent * AIR_BOOST_ACCELERATION else 0.0
            currentSpeed += acceleration * TIME_STEP
            if (currentSpeed > SUPERSONIC_SPEED) {
                currentSpeed = SUPERSONIC_SPEED
            }
            distanceSoFar += currentSpeed * TIME_STEP
            secondsSoFar += TIME_STEP
            boostRemaining -= BOOST_CONSUMED_PER_SECOND * TIME_STEP
            plot.addSlice(DistanceTimeSpeed(distanceSoFar, Duration.ofSeconds(secondsSoFar), currentSpeed))

            if (currentSpeed >= SUPERSONIC_SPEED) {
                // It gets boring from zero on. Put a slice at the very end.
                val secondsRemaining = secondsToSimulate - secondsSoFar
                plot.addSlice(DistanceTimeSpeed(distanceSoFar + SUPERSONIC_SPEED * secondsRemaining, duration, SUPERSONIC_SPEED))
                break
            }
        }

        return plot
    }
}
