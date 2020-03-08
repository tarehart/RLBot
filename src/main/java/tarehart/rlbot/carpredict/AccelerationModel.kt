package tarehart.rlbot.carpredict

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object AccelerationModel {

    val SUPERSONIC_SPEED = 46F
    val MEDIUM_SPEED = 28F
    val FLIP_THRESHOLD_SPEED = 20F

    private val TIME_STEP = 0.1F
    private val FRONT_FLIP_SPEED_BOOST = 10.0F
    private val FRONT_FLIP_SECONDS = 1.4F

    private val INCREMENTAL_BOOST_ACCELERATION = 19
    private val AIR_BOOST_ACCELERATION = 19.0F // It's a tiny bit faster, but account for course correction wiggle
    private val BOOST_CONSUMED_PER_SECOND = 25.0F

    // Thanks @chip#7643 for these uu values
    private val BRAKING_DECELERATION = -3500F / Vector3.PACKET_DISTANCE_TO_CLASSIC
    private val COASTING_DECELERATION = -525F / Vector3.PACKET_DISTANCE_TO_CLASSIC
    private val FULL_THROTTLE_ACCEL_AT_0 = 1600F / Vector3.PACKET_DISTANCE_TO_CLASSIC
    private val FULL_THROTTLE_TOP_SPEED = 1410F / Vector3.PACKET_DISTANCE_TO_CLASSIC
    private val BOOST_ACCEL = 991.66F / Vector3.PACKET_DISTANCE_TO_CLASSIC // I know we have 3 values for boost now... :/


    /*
     * Negative throttle: Braking has exactly 3500uu/s²
     * 0 Throttle: Coasting slows you down at 525uu/s²
     * Throttle > 0: Depends on current velocity, from 1600uu/s² at 0, to 0 acceleration when speed is 1410uu/s
     * Boosting adds an additional 991.6666uu/s² always
     */

    /**
     * Gets the acceleration in UU/s (ReliefBot units) with the throttle input and current velocity
     */
    fun getAccelerationWithThrottle(throttle: Number, carToManipulate: CarData): Float {
        val t = throttle.toFloat()
        val forwardSpeed = carToManipulate.velocity.dotProduct(carToManipulate.orientation.noseVector)
        if (t < 0) return BRAKING_DECELERATION
        else if (t > 0) {
            // Acceleration at full speed is 0
            val maxAccel = FULL_THROTTLE_ACCEL_AT_0 * (FULL_THROTTLE_TOP_SPEED - forwardSpeed) / FULL_THROTTLE_TOP_SPEED
            return t * maxAccel
        } else { // throttle == 0
            return COASTING_DECELERATION
        }
    }

    fun getThrottleForAcceleration(desiredAcceleration: Float, carToManipulate: CarData): Float {
        val forwardSpeed = carToManipulate.velocity.dotProduct(carToManipulate.orientation.noseVector)
        if (desiredAcceleration < 0) {
            // Get the closest one out of braking or coasting, hopefully this works. I've no idea if it will
            if (abs(desiredAcceleration - COASTING_DECELERATION) > abs(desiredAcceleration - BRAKING_DECELERATION)) {
                return 0F
            } else {
                return -1F
            }
        } else {
            // Desired acceleration of 0 requires we throttle a little bit.
            val maxAccel = FULL_THROTTLE_ACCEL_AT_0 * (FULL_THROTTLE_TOP_SPEED - forwardSpeed) / FULL_THROTTLE_TOP_SPEED
            return max(min(1F, desiredAcceleration / maxAccel), 0.001F)
        }
    }

    fun getAccelerationForDesiredSpeed(desiredSpeed: Float, carToManipulate: CarData, accelerationTime: Duration = Duration.ofMillis(64)) : Float {
        val forwardSpeed = carToManipulate.velocity.dotProduct(carToManipulate.orientation.noseVector)
        val timeToChangeSpeed = accelerationTime.seconds
        val changeInVelocity = desiredSpeed - forwardSpeed
        val desiredAcceleration = changeInVelocity / timeToChangeSpeed
        return desiredAcceleration
    }

    fun getThrottleForDesiredSpeed(desiredSpeed: Float, carToManipulate: CarData, accelerationTime: Duration = Duration.ofMillis(64) ) : Float {
        val desiredAcceleration = getAccelerationForDesiredSpeed(desiredSpeed, carToManipulate, accelerationTime)
        return getThrottleForAcceleration(desiredAcceleration, carToManipulate)
    }

    /**
     * Return control output to reach desired speed
     * Will attempt to solve for boost if permitted
     */
    fun getControlsForDesiredSpeed(desiredSpeed: Float, carToManipulate: CarData, accelerationTime: Duration = Duration.ofMillis(64), finesse: ControlFinesse = ControlFinesse() ) : AgentOutput {
        // Don't solve for boost if our acceleration time is greater than the max amount of boost we have
        // Assuming that acceleration time is how long we are intending to distribute the acceleration over, the boost wont last.
        val shouldSolveBoost = finesse.allowBoost && (carToManipulate.boost >= (BOOST_CONSUMED_PER_SECOND * accelerationTime.seconds))

        val solutionWithoutBoost = getThrottleForDesiredSpeed(desiredSpeed, carToManipulate, accelerationTime)

        if (shouldSolveBoost && (1.0 - solutionWithoutBoost) < 0.01) {
            val accelerationForSpeed = getAccelerationForDesiredSpeed(desiredSpeed, carToManipulate, accelerationTime)
            val accelerationWithThrottle = getAccelerationWithThrottle(solutionWithoutBoost, carToManipulate)
            if (accelerationWithThrottle < accelerationForSpeed) {
                // The acceleration we need cannot be delivered from throttle
                // We will calculate the throttle required LESS the boost acceleration, and see if that helps
                // We should not overshoot with boost, its better to conserve the boost than overshoot and decelerate (in my current opinion)
                val throttleAcceleration = accelerationForSpeed - BOOST_ACCEL
                val boostedThrottle = getThrottleForAcceleration(throttleAcceleration, carToManipulate)
                val boostedThrottleAccel = getAccelerationWithThrottle(boostedThrottle, carToManipulate)
                // Maybe do some logic on this to better decide of we should boost.
                if (boostedThrottle >= solutionWithoutBoost) {
                    return getOutputWithFinesse( AgentOutput().withThrottle(boostedThrottle).withBoost(), carToManipulate, finesse)
                }
            }
        }


        return getOutputWithFinesse(AgentOutput().withThrottle(solutionWithoutBoost), carToManipulate, finesse)
    }

    fun getOutputWithFinesse(output: AgentOutput, target: CarData, finesse: ControlFinesse) : AgentOutput {
        if (!finesse.allowBoost) {
            output.withBoost(false)
        }

        // If the direction of throttle is not equal to the direction of travel and we aren't permitted to brake
        // Then cancel it out
        if (!finesse.allowBrake && target.orientation.noseVector.dotProduct(target.velocity) * output.throttle < 0) {
            output.withThrottle(0.0)
        }
        return output
    }

    fun getTravelTime(carData: CarData, plot: DistancePlot, target: Vector3): Duration? {
        val distance = carData.position.distance(target)
        val travelTime = plot.getTravelTime(distance)
        val penaltySeconds = getSteerPenaltySeconds(carData, target)
        return travelTime?.plusSeconds(penaltySeconds)
    }

    fun getOrientDuration(carData: CarData, space: Vector3): Duration {
        return Duration.ofSeconds(getSteerPenaltySeconds(carData, space))
    }

    fun getSteerPenaltySeconds(carData: CarData, target: Vector3): Double {
        return getSteerPenaltySeconds(carData, (target - carData.position).flatten())
    }

    /**
     * Only works on flat ground.
     */
    fun getSteerPenaltySeconds(carData: CarData, newDirection: Vector2): Double {
        val direction = newDirection.normalized()
        val nominalSpeed = getNominalSpeed(carData)
        val speedAlongDirection = carData.velocity.flatten().dotProduct(direction)
        if (speedAlongDirection < nominalSpeed) {
            val orientError = 0.5 * Math.max( 0.0, 0.8 - direction.dotProduct(carData.orientation.noseVector.flatten().normalized()))
            val speedDeficit = nominalSpeed - speedAlongDirection
            return speedDeficit / FULL_THROTTLE_ACCEL_AT_0 + orientError
        }
        return 0.0
    }

    private fun getNominalSpeed(car: CarData): Float {
        return if (car.hasWheelContact)
            ManeuverMath.forwardSpeed(car)
        else
            car.velocity.flatten().magnitude()
    }

    @JvmOverloads
    fun simulateAcceleration(carData: CarData, duration: Duration, boostBudget: Float, flipCutoffDistance: Number = Float.MAX_VALUE): DistancePlot {

        var currentSpeed = getNominalSpeed(carData)

        val plot = DistancePlot(DistanceTimeSpeed(0F, Duration.ofMillis(0), currentSpeed))

        var boostRemaining = boostBudget

        var distanceSoFar = 0.0F
        var secondsSoFar = 0.0F

        val secondsToSimulate = duration.seconds

        while (secondsSoFar < secondsToSimulate) {
            val hypotheticalFrontFlipDistance = getFrontFlipDistance(currentSpeed)


            if (currentSpeed >= SUPERSONIC_SPEED) {
                // It gets boring from zero on. Put a slice at the very end.
                val secondsRemaining = secondsToSimulate - secondsSoFar
                plot.addSlice(DistanceTimeSpeed(distanceSoFar + SUPERSONIC_SPEED * secondsRemaining, duration, SUPERSONIC_SPEED))
                break
            } else if (boostRemaining <= 0 && currentSpeed > FLIP_THRESHOLD_SPEED && distanceSoFar + hypotheticalFrontFlipDistance < flipCutoffDistance.toFloat()) {
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
                val nextSpeed = min(SUPERSONIC_SPEED, currentSpeed + acceleration * TIME_STEP)
                boostRemaining -= BOOST_CONSUMED_PER_SECOND * TIME_STEP
                distanceSoFar += ((currentSpeed + nextSpeed) / 2) * TIME_STEP
                secondsSoFar += TIME_STEP
                currentSpeed = nextSpeed
                plot.addSlice(DistanceTimeSpeed(distanceSoFar, Duration.ofSeconds(secondsSoFar), currentSpeed))
            }
        }

        return plot
    }

    private fun getAcceleration(currentSpeed: Float, hasBoost: Boolean): Float {

        if (currentSpeed >= SUPERSONIC_SPEED || !hasBoost && currentSpeed >= MEDIUM_SPEED) {
            return 0F
        }

        var accel = 0F
        if (currentSpeed < MEDIUM_SPEED) {
            accel = 30 - currentSpeed * .95F
        }

        if (hasBoost) {
            accel += INCREMENTAL_BOOST_ACCELERATION
        }

        return accel
    }

    fun getFrontFlipDistance(speed: Float): Float {
        return Math.min(SUPERSONIC_SPEED, speed + FRONT_FLIP_SPEED_BOOST) * FRONT_FLIP_SECONDS
    }

    fun simulateAirAcceleration(car: CarData, duration: Duration, horizontalPitchComponent: Float): DistancePlot {
        var currentSpeed = car.velocity.flatten().magnitude()
        val plot = DistancePlot(DistanceTimeSpeed(0F, Duration.ofMillis(0), currentSpeed))

        var boostRemaining = car.boost

        var distanceSoFar = 0F
        var secondsSoFar = 0F

        val secondsToSimulate = duration.seconds

        while (secondsSoFar < secondsToSimulate) {

            val acceleration = if (boostRemaining > 0) horizontalPitchComponent * AIR_BOOST_ACCELERATION else 0F
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
