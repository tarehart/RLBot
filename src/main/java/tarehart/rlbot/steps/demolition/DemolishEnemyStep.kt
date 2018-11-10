package tarehart.rlbot.steps.demolition

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.carpredict.CarInterceptPlanner
import tarehart.rlbot.carpredict.CarPredictor
import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color

class DemolishEnemyStep : NestedPlanStep() {

    enum class DemolishPhase {
        CHASE,
        AWAIT_LIFTOFF,
        JUMP,
        DODGE,
        LAND
    }

    class WheelContactWatcher(val carIndex: Int) {
        var previouslyHadWheelContact: Boolean? = null
        fun justJumped(input: AgentInput): Boolean {
            return previouslyHadWheelContact == true && inputChanged(input)
        }

        fun justLanded(input: AgentInput): Boolean {
            return previouslyHadWheelContact == false && inputChanged(input)
        }

        private fun inputChanged(input: AgentInput): Boolean {
            val car = input.allCars[carIndex]
            val changed = previouslyHadWheelContact != null && previouslyHadWheelContact != car.hasWheelContact
            previouslyHadWheelContact = car.hasWheelContact
            return changed
        }
    }

    class DemolishTransition(val output: AgentOutput, val phase: DemolishPhase)

    private var canDodge: Boolean = true
    //private var enemyIndex: Int? = null
    private var momentJumped: GameTime? = null
    private val SECONDS_BEFORE_CONTACT_TO_JUMP = 0.8
    private val SECONDS_TO_PREDICT = 4.0
    private val NEEDS_JUMP_HEIGHT = 0.5
    private var demolishPhase = DemolishPhase.CHASE
    private lateinit var selfContactWatcher: WheelContactWatcher
    private lateinit var carPredictor: CarPredictor
    private var enemyWatcher: WheelContactWatcher? = null


    override fun doInitialComputation(input: AgentInput) {
        super.doInitialComputation(input)

        val car = input.myCarData
        val oppositeTeam = input.getTeamRoster(input.team.opposite())

        val enemyCar = enemyWatcher?.let { detector -> oppositeTeam.first { it.playerIndex == detector.carIndex } } ?:
            oppositeTeam.filter { !it.isDemolished }.minBy { car.position.distance(it.position) } ?: return

        if (!::carPredictor.isInitialized) {
            carPredictor = CarPredictor(enemyCar.playerIndex)
        }

        val path = carPredictor.predictCarMotion(input, Duration.ofSeconds(SECONDS_TO_PREDICT))
        val renderer = BotLoopRenderer.forBotLoop(input.bot)
        path.renderIn3d(renderer)

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(SECONDS_TO_PREDICT), car.boost)
        val carIntercept = CarInterceptPlanner.getCarIntercept(car, path, distancePlot)

        carIntercept?.let {
            RenderUtil.drawSphere(renderer, it.space, 1.0, Color.RED)
        }
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        if (!::selfContactWatcher.isInitialized) {
            selfContactWatcher = WheelContactWatcher(input.playerIndex)
        }

        val car = input.myCarData
        val oppositeTeam = input.getTeamRoster(input.team.opposite())

        val enemyCar = enemyWatcher?.let { detector -> oppositeTeam.first { it.playerIndex == detector.carIndex } } ?:
        oppositeTeam.filter { !it.isDemolished }.minBy { car.position.distance(it.position) }

        if (enemyCar == null || enemyCar.isDemolished) {
            return null
        }

        if (car.hasWheelContact && !(demolishPhase == DemolishPhase.CHASE || demolishPhase == DemolishPhase.AWAIT_LIFTOFF)) {
            return null // We already attempted a midair dodge, and now we've hit the ground. Give up.
        }

        if (enemyCar.position.distance(car.position) < 30) {
            enemyWatcher = WheelContactWatcher(enemyCar.playerIndex) // Commit to demolishing this particular enemy
        }

        val transition = when (demolishPhase) {

            DemolishPhase.CHASE -> chase(input, enemyCar) ?: return null

            DemolishPhase.AWAIT_LIFTOFF -> DemolishTransition(
                    AgentOutput().withBoost().withJump(),
                    if (!car.hasWheelContact) DemolishPhase.JUMP else DemolishPhase.AWAIT_LIFTOFF)

            DemolishPhase.JUMP -> jump(input, enemyCar) ?: return null

            else -> DemolishTransition(AgentOutput().withThrottle(1.0), demolishPhase)
        }

        demolishPhase = transition.phase
        return transition.output
    }

    private fun chase(input: AgentInput, enemyCar: CarData): DemolishTransition? {
        val car = input.myCarData

        if (!hasEnoughBoost(car)) {
            return null
        }

        val renderer = BotLoopRenderer.forBotLoop(input.bot)

        val path = carPredictor.predictCarMotion(input, Duration.ofSeconds(SECONDS_TO_PREDICT))
        path.renderIn3d(renderer)
        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(SECONDS_TO_PREDICT), car.boost)
        val carIntercept = CarInterceptPlanner.getCarIntercept(car, path, distancePlot)

        carIntercept?.let {
            RenderUtil.drawSphere(renderer, it.space, 1.0, Color.RED)

            val secondsTillContact = Duration.between(car.time, it.time).seconds
            if (secondsTillContact <= SECONDS_BEFORE_CONTACT_TO_JUMP && !enemyCar.hasWheelContact && it.space.z > NEEDS_JUMP_HEIGHT) {
                momentJumped = car.time
                return DemolishTransition(AgentOutput().withBoost().withJump(), DemolishPhase.AWAIT_LIFTOFF)
            }

            return DemolishTransition(SteerUtil.steerTowardGroundPosition(car, it.space), DemolishPhase.CHASE)
        }

        return DemolishTransition(SteerUtil.steerTowardGroundPositionGreedily(car, enemyCar.position.flatten()), DemolishPhase.CHASE)
    }

    private fun jump(input: AgentInput, enemyCar: CarData): DemolishTransition? {

        val car = input.myCarData

        val simulationDuration = Duration.ofSeconds(SECONDS_BEFORE_CONTACT_TO_JUMP * 1.5)

        val path = carPredictor.predictCarMotion(input, simulationDuration)
        val renderer = BotLoopRenderer.forBotLoop(input.bot)
        path.renderIn3d(renderer)
        val distancePlot = AccelerationModel.simulateAcceleration(car, simulationDuration, car.boost)
        val carIntercept = CarInterceptPlanner.getCarIntercept(car, path, distancePlot)

        if (carIntercept != null) {

            RenderUtil.drawSphere(renderer, carIntercept.space, 1.0, Color.RED)

            val needsDoubleJump = carIntercept.space.z > 5

            val toIntercept = carIntercept.space - car.position

            if (needsDoubleJump && canDodge && !car.hasWheelContact) {
                return workTowardDoubleJump(car, input)
            }

            val steerCorrection = car.velocity.flatten().correctionAngle(toIntercept.flatten())
            if (Math.abs(steerCorrection) > Math.PI / 20) {
                val immediateOutput = startPlan(Plan().unstoppable()
                        .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withBoost()))
                        .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()
                                .withBoost()
                                .withRoll(-Math.signum(steerCorrection))
                                .withJump())), input) ?: return null
                return DemolishTransition(immediateOutput, DemolishPhase.DODGE)
            }
        }

        RenderUtil.drawSphere(renderer, enemyCar.position, 1.0, Color.GRAY)

        return DemolishTransition(AgentOutput().withJump().withBoost(), DemolishPhase.JUMP)
    }

    private fun workTowardDoubleJump(car: CarData, input: AgentInput): DemolishTransition? {
        val timeSinceJump = Duration.between(momentJumped!!, car.time)
        if (timeSinceJump < Duration.ofMillis(200)) {
            // Don't double jump yet, we need to get more benefit from our initial jump.
            return DemolishTransition(AgentOutput().withJump(), DemolishPhase.JUMP)
        }

        canDodge = false
        val immediateOutput = startPlan(Plan().unstoppable()
                .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withBoost()))
                .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withBoost().withJump())), input)
                ?: return null

        return DemolishTransition(immediateOutput, DemolishPhase.DODGE)
    }

    override fun getLocalSituation(): String {
        return "Demolishing enemy"
    }

    companion object {
        fun hasEnoughBoost(car: CarData): Boolean {
            return car.isSupersonic || car.boost > 15
        }
    }
}
