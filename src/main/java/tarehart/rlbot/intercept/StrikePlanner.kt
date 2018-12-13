package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.*
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

object StrikePlanner {

    const val BOOST_NEEDED_FOR_AERIAL = 20.0
    const val NEEDS_AERIAL_THRESHOLD = ManeuverMath.MASH_JUMP_HEIGHT
    const val MAX_JUMP_HIT = NEEDS_AERIAL_THRESHOLD
    const val CAR_BASE_HEIGHT = ManeuverMath.BASE_CAR_Z

    fun checkLaunchReadiness(checklist: LaunchChecklist, car: CarData, intercept: SpaceTime) {

        val correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, intercept.space)
        val secondsTillIntercept = Duration.between(car.time, intercept.time).seconds

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 30 &&
                Math.abs(car.spin.yawRate) < 2.0
        checklist.closeEnough = secondsTillIntercept < 4

        checklist.upright = car.orientation.roofVector.dotProduct(Vector3(0.0, 0.0, 1.0)) > .85
        checklist.onTheGround = car.hasWheelContact
    }

    fun isVerticallyAccessible(carData: CarData, intercept: SpaceTime): Boolean {
        val secondsTillIntercept = Duration.between(carData.time, intercept.time).seconds

        if (intercept.space.z < NEEDS_AERIAL_THRESHOLD) {
            val tMinus = secondsTillIntercept - JumpHitStrike(intercept.space.z).strikeDuration.seconds
            return tMinus >= -0.1
        }

        if (carData.boost > BOOST_NEEDED_FOR_AERIAL) {
            val tMinus = AerialStrike.getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept)
            return tMinus >= -0.1
        }
        return false
    }



    fun computeStrikeStyle(intercept: Vector3, approachAngleMagnitude: Double): StrikeProfile.Style {

        if (approachAngleMagnitude < Math.PI / 16) {

            val height = intercept.z
            if (height <= ChipStrike.MAX_HEIGHT_OF_BALL_FOR_CHIP) {
                return StrikeProfile.Style.CHIP
            }

            if (FlipHitStrike.isVerticallyAccessible(height)) {
                return StrikeProfile.Style.FLIP_HIT
            }
            if (height < StrikePlanner.MAX_JUMP_HIT) {
                return StrikeProfile.Style.JUMP_HIT
            }
        } else if (approachAngleMagnitude < Math.PI * .2 && intercept.z < ChipStrike.MAX_HEIGHT_OF_BALL_FOR_CHIP) {
            return StrikeProfile.Style.CHIP
        }

        if (intercept.z < StrikePlanner.MAX_JUMP_HIT) {
            val isNearGoal = intercept.distance(GoalUtil.getNearestGoal(intercept).center) < 50
            if (approachAngleMagnitude > Math.PI / 4 && isNearGoal) {
                return StrikeProfile.Style.SIDE_HIT
            }
            return StrikeProfile.Style.DIAGONAL_HIT
        }
        return StrikeProfile.Style.AERIAL
    }

    fun getStrikeProfile(style: StrikeProfile.Style, height: Double): StrikeProfile {
        return when(style) {
            StrikeProfile.Style.CHIP -> ChipStrike()
            StrikeProfile.Style.JUMP_HIT -> JumpHitStrike(height)
            StrikeProfile.Style.FLIP_HIT -> FlipHitStrike()
            StrikeProfile.Style.DIAGONAL_HIT -> DiagonalStrike(height)
            StrikeProfile.Style.SIDE_HIT -> SideHitStrike(height)
            StrikeProfile.Style.AERIAL -> AerialStrike(height)
            StrikeProfile.Style.DRIBBLE -> DribbleStrike()
        }
    }

    fun getBoostBudget(carData: CarData): Double {
        return carData.boost - BOOST_NEEDED_FOR_AERIAL - 5.0
    }
}
