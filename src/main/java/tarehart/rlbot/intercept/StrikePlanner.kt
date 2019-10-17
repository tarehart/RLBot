package tarehart.rlbot.intercept

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.*
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.defense.ThreatAssessor
import tarehart.rlbot.steps.strikes.KickStrategy
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

        if (carData.boost > boostNeededForAerial(intercept.space.z)) {
            val tMinus = AerialStrike.getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept)
            return tMinus >= -0.1
        }
        return false
    }

    fun boostNeededForAerial(height: Double) : Double {
        return if (height > StrikePlanner.NEEDS_AERIAL_THRESHOLD) StrikePlanner.BOOST_NEEDED_FOR_AERIAL + height else 0.0
    }


    fun computeStrikeStyle(slice: BallSlice, approachAngleMagnitude: Double): StrikeProfile.Style {
        val height = slice.space.z
        if (height > StrikePlanner.MAX_JUMP_HIT) {
            return StrikeProfile.Style.AERIAL
        }

        if (approachAngleMagnitude < Math.PI / 16) {
            if (height <= ChipStrike.MAX_HEIGHT_OF_BALL_FOR_CHIP) {
                return StrikeProfile.Style.CHIP
            }

            if (FlipHitStrike.isVerticallyAccessible(height)) {
                return StrikeProfile.Style.FLIP_HIT
            }
            if (height < StrikePlanner.MAX_JUMP_HIT) {
                return StrikeProfile.Style.JUMP_HIT
            }
        } else if (approachAngleMagnitude < Math.PI * .2 &&
                height < ChipStrike.MAX_HEIGHT_OF_BALL_FOR_CHIP) {
            return StrikeProfile.Style.CHIP
        }

        if (approachAngleMagnitude > Math.PI / 4) {
            return StrikeProfile.Style.SIDE_HIT
        }
        return StrikeProfile.Style.DIAGONAL_HIT
    }

    fun computeStrikeProfile(height: Double): StrikeProfile {
        if (height <= ChipStrike.MAX_HEIGHT_OF_BALL_FOR_CHIP) {
            return ChipStrike()
        }

        if (FlipHitStrike.isVerticallyAccessible(height)) {
            return FlipHitStrike()
        }
        if (height < StrikePlanner.MAX_JUMP_HIT) {
            return JumpHitStrike(height)
        }

        return AerialStrike(height, null)
    }

    fun getStrikeProfile(style: StrikeProfile.Style, height: Double, kickStrategy: KickStrategy): StrikeProfile {
        return when(style) {
            StrikeProfile.Style.CHIP -> ChipStrike()
            StrikeProfile.Style.JUMP_HIT -> JumpHitStrike(height)
            StrikeProfile.Style.FLIP_HIT -> FlipHitStrike()
            StrikeProfile.Style.DIAGONAL_HIT -> DiagonalStrike(height)
            StrikeProfile.Style.SIDE_HIT -> SideHitStrike(height)
            StrikeProfile.Style.AERIAL -> AerialStrike(height, kickStrategy)
            StrikeProfile.Style.DRIBBLE -> DribbleStrike()
            StrikeProfile.Style.DOUBLE_JUMP_POKE -> DoubleJumpPokeStrike(height)
        }
    }

    fun getBoostBudget(carData: CarData): Double {
        return carData.boost - BOOST_NEEDED_FOR_AERIAL - 5.0
    }
}
