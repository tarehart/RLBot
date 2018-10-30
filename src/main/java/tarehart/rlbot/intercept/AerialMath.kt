package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.time.Duration

object AerialMath {

    val EFFECTIVE_AIR_BOOST_ACCELERATION = 18.0
    val JUMP_ASSIST_DURATION = .6
    val JUMP_ASSIST = 12
    val TYPICAL_UPWARD_ACCEL = (EFFECTIVE_AIR_BOOST_ACCELERATION - ArenaModel.GRAVITY) * .5 // Assume a 30 degree upward angle

    fun getDesiredZComponentBasedOnAccel(targetHeight: Double, timeTillIntercept: Duration, timeSinceLaunch: Duration, car: CarData): Double {

        val additionalHeight = targetHeight - car.position.z
        val secondsTillIntercept = timeTillIntercept.seconds
        val averageVelNeeded = additionalHeight / secondsTillIntercept
        val initialVel = car.velocity.z
        val extraUpwardVelForFinesse = 1.0
        val finalVel = initialVel + (averageVelNeeded - initialVel) * 2 + extraUpwardVelForFinesse
        val averageAccel = (finalVel - initialVel) / secondsTillIntercept

        val secondsSinceLaunch = timeSinceLaunch.seconds
        val jumpAssistSecondsRemaining = Math.max(0.0, JUMP_ASSIST_DURATION - secondsSinceLaunch)
        val proportionAssistedByJump = jumpAssistSecondsRemaining / (jumpAssistSecondsRemaining + secondsTillIntercept)

        val boostAccelToComplementJumpAssist = averageAccel - (JUMP_ASSIST - ArenaModel.GRAVITY)
        val boostAccelNeededPostJump = averageAccel + ArenaModel.GRAVITY

        val averageBoostAccelNeeded = boostAccelToComplementJumpAssist * proportionAssistedByJump + boostAccelNeededPostJump * (1 - proportionAssistedByJump)

        return Clamper.clamp(averageBoostAccelNeeded / EFFECTIVE_AIR_BOOST_ACCELERATION, -1.0, 1.0)
    }

    fun getProjectedHeight(car: CarData, secondsTillIntercept: Double, secondsSinceLaunch: Double): Double {

        val initialHeight = car.position.z
        val initialVelocity = car.velocity.z
        val noseVertical = car.orientation.noseVector.z

        val phase2Height: Double
        val phase2Velocity: Double

        val tjmp = Math.max(0.0, JUMP_ASSIST_DURATION - secondsSinceLaunch)
        if (tjmp > 0) {
            val verticalAcceleration = EFFECTIVE_AIR_BOOST_ACCELERATION * noseVertical + JUMP_ASSIST - ArenaModel.GRAVITY
            phase2Height = initialHeight + initialVelocity * tjmp + .5 * verticalAcceleration * tjmp * tjmp
            phase2Velocity = initialVelocity + verticalAcceleration * tjmp
        } else {
            phase2Height = initialHeight
            phase2Velocity = initialVelocity
        }

        val tfly = secondsTillIntercept - tjmp

        val verticalAcceleration = EFFECTIVE_AIR_BOOST_ACCELERATION * noseVertical - ArenaModel.GRAVITY

        return phase2Height + phase2Velocity * tfly + .5 * verticalAcceleration * tfly * tfly
    }

    fun timeToAir(height: Double): Double {
        val a = TYPICAL_UPWARD_ACCEL // Upward acceleration
        val b = 10 // Initial upward velocity from jump
        val c = -(height - AirTouchPlanner.CAR_BASE_HEIGHT)
        val liftoffDelay = 0.5
        return (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a) + liftoffDelay
    }

}
