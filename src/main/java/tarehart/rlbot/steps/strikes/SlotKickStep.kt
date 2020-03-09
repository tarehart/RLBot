package tarehart.rlbot.steps.strikes

import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.physics.ChipOption
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println
import java.awt.Color

class SlotKickStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {

    private var slotStart: Vector3? = null
    private var slotEnd: Vector3? = null
    private var favoredChipOption: ChipOption? = null
    private var favoredOffset: Vector3? = null

    private val disruptionMeter = BallPathDisruptionMeter(3)

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

        val ballCenterToCarCenter = favoredOffset ?: Vector3()
        val intercept = InterceptCalculator.getFilteredInterceptOpportunity(car, ballPath, distancePlot, ballCenterToCarCenter, overallPredicate) ?: return null

        if (slotStart == null) {
            val chipOptions = BallPhysics.computeChipOptions(car.position, intercept.accelSlice.speed, intercept.ballSlice,
                    car.hitbox, (-25..25).map { it * .1F })

            for ((index, chipOption) in chipOptions.withIndex()) {
                val color = RenderUtil.rainbowColor(index)
                car.renderer.drawLine3d(color, intercept.ballSlice.space, intercept.ballSlice.space + chipOption.velocity)
                chipOption.carSlice.render(car.renderer, color)
            }
        }

        slotEnd = intercept.space.withZ(car.position.z)

        val steerCorrection = SteerUtil.getCorrectionAngleRad(car, intercept.space)
        val firmStart = slotStart
        val firmEnd = intercept.space
        if (Math.abs(steerCorrection) < 0.03) {
            val idealDirection = kickStrategy.getKickDirection(car, intercept.space) ?: return null

            val chipOption = BallPhysics.computeBestChipOption(car.position, intercept.accelSlice.speed,
                    intercept.ballSlice, car.hitbox, idealDirection)
            favoredChipOption = chipOption
            favoredOffset = chipOption.carSlice.space - intercept.ballSlice.space
            slotStart = car.position
        }

        if (firmStart != null) {
            val alignment = Vector2.alignment(firmStart.flatten(), car.position.flatten(), firmEnd.flatten())
            if (alignment < .9) {
                BotLog.println("Car fell out of the slot!", car.playerIndex)
                return null
            }

            favoredChipOption?.let {
                val renderer = NamedRenderer("slotKick")
                renderer.startPacket()
                renderer.drawLine3d(Color.GREEN, intercept.ballSlice.space, intercept.ballSlice.space + it.velocity)
                it.carSlice.render(renderer, Color.GREEN)
                RenderUtil.drawCircle(renderer, it.chipCircle, it.impactPoint.z, Color.WHITE)
                renderer.finishAndSend()
            }
        }

        drawSlot(car)

        if (car.boost < 1 && bundle.tacticalSituation.ballAdvantage.seconds > 0) {
            SteerUtil.getSensibleFlip(car, intercept.space)?.let {
                println("Front flip toward slot kick", bundle.agentInput.playerIndex)
                return startPlan(it, bundle)
            }
        }

        val toIntercept = intercept.space.flatten() - car.position.flatten()

        return SteerUtil.steerTowardGroundPosition(
                car, car.position.flatten() + toIntercept * 1.5F,
                detourForBoost = slotStart == null && car.boost < 20,
                conserveBoost = false)
    }

    fun drawSlot(car: CarData) {
        val start = slotStart
        val end = slotEnd ?: return
        if (start != null) {
            car.renderer.drawLine3d(Color.GREEN, start, end)
        } else {
            car.renderer.drawLine3d(Color.ORANGE,
                    car.position,
                    car.position.plus(car.orientation.noseVector.scaledToMagnitude(5)))
            car.renderer.drawLine3d(Color.ORANGE,
                    car.position,
                    car.position.plus((end - car.position).withZ(0).scaledToMagnitude(5)))
        }
    }

    override fun getLocalSituation(): String {
        return "Slot kick - " + kickStrategy.javaClass.simpleName
    }
}
