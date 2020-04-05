package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.strike.DodgelessJumpStrike
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.intercept.strike.Style
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ChipOption
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println


class FlexibleKickStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {

    private var slotStart: Vector3? = null
    private var slotEnd: Vector3? = null
    private var favoredChipOption: ChipOption? = null
    private var favoredSliceToCar: Vector3? = null
    private var isFinalMoments = false

    private val disruptionMeter = BallPathDisruptionMeter(5)

    override fun reset() {
        super.reset()
        slotStart = null
        slotEnd = null
        disruptionMeter.reset()
    }

    override fun canInterrupt(): Boolean {
        return super.canInterrupt() && !isFinalMoments
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val ballPath = bundle.tacticalSituation.ballPath

        if (disruptionMeter.isDisrupted(ballPath)) {
            BotLog.println("Ball path disrupted during bent kick.", car.playerIndex)
            cancelPlan = true
            return null
        }

        val overallPredicate = { cd: CarData, st: SpaceTime ->
            val verticallyAccessible = StrikePlanner.isVerticallyAccessible(cd, st)
            val viableKick = kickStrategy.looksViable(cd, st.space)
            verticallyAccessible && viableKick
        }

        val distancePlot = bundle.tacticalSituation.expectedContact.distancePlot

        val sliceToCar = favoredSliceToCar ?: Vector3()
        val intercept = InterceptCalculator.getFilteredInterceptOpportunity(car, ballPath, distancePlot, sliceToCar, overallPredicate,
                strikeProfileFn = { height -> selectStrike(height) }) ?:
        return null

        val kickPlan = DirectedKickUtil.planKickFromIntercept(intercept, ballPath, car, kickStrategy)

        if (kickPlan == null) {
            println("Bent kick failed to make kick plan", car.playerIndex)
            return null
        }

        if (kickPlan.launchPad.isPlausibleFinalApproach(car)) {
            kickPlan.intercept.strikeProfile.getPlanFancy(car, kickPlan)?.let {
                println("Launched at %s %s with speed %s"
                        .format(bundle.agentInput.time, car.position, car.velocity.magnitude()),
                        car.playerIndex)
                return startPlan(it, bundle)
            }
        }

        return kickPlan.launchPad.planRoute(car, distancePlot)
    }

    private fun selectStrike(height: Float): StrikeProfile {
        return DodgelessJumpStrike(height)
        // TODO: make use of getStrikeProfile fn
    }

    override fun getLocalSituation(): String {
        return "Bent kick - " + kickStrategy.javaClass.simpleName
    }

    companion object {
        fun getStrikeProfile(slice: BallSlice, approachAngleMagnitude: Float, kickStrategy: KickStrategy,
                             styleHint: Style?, bundle: TacticalBundle): StrikeProfile {

            if (styleHint == Style.SIDE_HIT || styleHint == Style.DIAGONAL_HIT) {
                // If we were going for a particular sideways hit, stay committed to it. Sometimes the
                // angle changes as a natural part of the leadup and we don't want to thrash based on that.
                return StrikePlanner.getStrikeProfile(styleHint, slice.space.z, kickStrategy)
            }
            val style = StrikePlanner.computeStrikeStyle(slice, approachAngleMagnitude)
            return StrikePlanner.getStrikeProfile(style, slice.space.z, kickStrategy)
        }
    }
}
