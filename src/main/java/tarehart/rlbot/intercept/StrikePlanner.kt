package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.*
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog

import java.util.Optional

object StrikePlanner {

    // This is the approximate angle a car needs to boost at to not accelerate up or down.
    private const val UPWARD_VELOCITY_MAINTENANCE_ANGLE = .25

    fun planImmediateLaunch(car: CarData, intercept: Intercept): Plan? {

        val height = intercept.space.z
        val strikeStyle = intercept.strikeProfile.style
        if (strikeStyle === StrikeProfile.Style.AERIAL) {
            val checklist = AirTouchPlanner.checkAerialReadiness(car, intercept)
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing Aerial!", car.playerIndex)

                val groundDistance = car.position.flatten().distance(intercept.space.flatten())
                val radiansForTilt = Math.atan2(height, groundDistance) + UPWARD_VELOCITY_MAINTENANCE_ANGLE

                val tiltBackSeconds = 0.2 + radiansForTilt * .1

                return if (Duration.between(car.time, intercept.time).seconds > 1.5 && intercept.space.z > 10) {
                    SetPieces.performDoubleJumpAerial(tiltBackSeconds * .8)
                } else SetPieces.performAerial(tiltBackSeconds)
            }
            return null
        }

        if (strikeStyle === StrikeProfile.Style.JUMP_HIT) {
            val checklist = AirTouchPlanner.checkJumpHitReadiness(car, intercept)
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing JumpHit!", car.playerIndex)
                return SetPieces.performJumpHit(intercept.strikeProfile.hangTime)
            }
            return null
        }

        if (strikeStyle === StrikeProfile.Style.SIDE_HIT) {
            val checklist = AirTouchPlanner.checkSideHitReadiness(car, intercept)
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing SideHit!", car.playerIndex)
                val toIntercept = intercept.space.flatten() - car.position.flatten()
                val left = car.orientation.noseVector.flatten().correctionAngle(toIntercept) > 0
                return SetPieces.jumpSideFlip(left, Duration.ofSeconds(intercept.strikeProfile.hangTime), intercept.spareTime.seconds <= 0)
            }
            return null
        }

        if (strikeStyle === StrikeProfile.Style.DIAGONAL_HIT) {
            val checklist = AirTouchPlanner.checkDiagonalHitReadiness(car, intercept)
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing DiagonalHit!", car.playerIndex)
                val toIntercept = intercept.space.flatten() - car.position.flatten()
                val left = car.orientation.noseVector.flatten().correctionAngle(toIntercept) > 0
                return SetPieces.diagonalFlip(left, Duration.ofSeconds(intercept.strikeProfile.hangTime))
            }
            return null
        }

        if (strikeStyle === StrikeProfile.Style.FLIP_HIT) {
            val checklist = AirTouchPlanner.checkFlipHitReadiness(car, intercept)
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing FlipHit!", car.playerIndex)
                return SetPieces.frontFlip()
            }
            return null
        }

        return null
    }
}
