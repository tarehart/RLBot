package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.intercept.strike.Style
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println
import java.awt.Color

class SlotKickStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {

    private var slotStart: Vector3? = null
    private var slotEnd: Vector3? = null

    private val disruptionMeter = BallPathDisruptionMeter(1)

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val ballPath = bundle.tacticalSituation.ballPath

        if (disruptionMeter.isDisrupted(ballPath)) {
            BotLog.println("Ball path disrupted during slot kick.", car.playerIndex)
            cancelPlan = true
            return null
        }

        val overallPredicate = { cd: CarData, st: SpaceTime ->
            val verticallyAccessible = StrikePlanner.isVerticallyAccessible(cd, st)
            val viableKick = kickStrategy.looksViable(cd, st.space)
            verticallyAccessible && viableKick
        }

        val distancePlot = bundle.tacticalSituation.expectedContact?.distancePlot ?: return null

        val ballCenterToCarCenter = Vector3() // TODO: figure this out nicely
        val intercept = InterceptCalculator.getFilteredInterceptOpportunity(car, ballPath, distancePlot, ballCenterToCarCenter, overallPredicate) ?: return null

        slotEnd = intercept.space.withZ(car.position.z)

        val steerCorrection = SteerUtil.getCorrectionAngleRad(car, intercept.space)
        val firmStart = slotStart
        val firmEnd = intercept.space
        if (firmStart == null && Math.abs(steerCorrection) < 0.03) {
            slotStart = car.position
        }

        if (firmStart != null) {
            val alignment = Vector2.alignment(firmStart.flatten(), car.position.flatten(), firmEnd.flatten())
            if (alignment < .9) {
                BotLog.println("Car fell out of the slot!", car.playerIndex)
                return null
            }
        }

        drawSlot(car)

        if (car.boost < 1 && bundle.tacticalSituation.ballAdvantage.seconds > 0) {
            SteerUtil.getSensibleFlip(car, intercept.space)?.let {
                println("Front flip toward slot kick", bundle.agentInput.playerIndex)
                return startPlan(it, bundle)
            }
        }

        return SteerUtil.steerTowardGroundPosition(car, intercept.space.flatten(), detourForBoost = slotStart == null, conserveBoost = false)
    }

    fun drawSlot(car: CarData) {
        val start = slotStart
        val end = slotEnd ?: return
        if (start != null) {
            car.renderer.drawLine3d(Color.GREEN, start.toRlbot(), end.toRlbot())
        } else {
            car.renderer.drawLine3d(Color.ORANGE,
                    car.position.toRlbot(),
                    car.position.plus(car.orientation.noseVector.scaledToMagnitude(5)).toRlbot())
            car.renderer.drawLine3d(Color.ORANGE,
                    car.position.toRlbot(),
                    car.position.plus((end - car.position).withZ(0).scaledToMagnitude(5)).toRlbot())
        }
    }

    override fun getLocalSituation(): String {
        return "Slot kick - " + kickStrategy.javaClass.simpleName
    }
}
