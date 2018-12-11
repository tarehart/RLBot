package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

class GoForKickoffStep : NestedPlanStep() {
    override fun getLocalSituation(): String {
        return "Going for kickoff"
    }

    private var kickoffType: KickoffType? = null
    private var counterAttack = false // Math.random() < .3
    private lateinit var startTime: GameTime

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        if (bundle.agentInput.ballPosition.flatten().magnitudeSquared() > 0) {
            return null
        }

        val car = bundle.agentInput.myCarData

        if (kickoffType == null) {
            kickoffType = getKickoffType(car)
            startTime = bundle.agentInput.time
        }

        if ((kickoffType == KickoffType.CENTER || kickoffType == KickoffType.CHEATIN) && counterAttack) {
            val goalLine = GoalUtil.getOwnGoal(bundle.agentInput.team).center.flatten()
            val toEnemy = goalLine.scaled(-1.0)
            return startPlan(Plan(Plan.Posture.NEUTRAL)
                    .withStep(ParkTheCarStep { _ -> PositionFacing(goalLine, toEnemy) })
                    .withStep(BlindStep(Duration.ofSeconds(0.5), AgentOutput())),
                    bundle)
        }

        val target: Vector2
        if (kickoffType == KickoffType.CHEATIN && Math.abs(car.position.y) > CHEATIN_BOOST_Y + 10) {
            // Steer toward boost
            val ySide = Math.signum(car.position.y)
            target = Vector2(0.0, ySide * CHEATIN_BOOST_Y)
            return SteerUtil.steerTowardGroundPosition(car, target, detourForBoost = false)
        }
        return startPlan(FirstViableStepPlan(Plan.Posture.NEUTRAL).withStep(ChallengeStep()).withStep(InterceptStep(Vector3())), bundle)
    }

    private enum class KickoffType {
        CENTER,
        CHEATIN,
        SLANTERD,
        UNKNOWN
    }

    private fun getKickoffType(car: CarData): KickoffType {
        val xPosition = Math.abs(car.position.x)
        val yPosition = Math.abs(car.position.y)
        if (getNumberDistance(CENTER_KICKOFF_X, xPosition) < WIGGLE_ROOM && getNumberDistance(CENTER_KICKOFF_Y, yPosition) < WIGGLE_ROOM) {
            BotLog.println("it be center", car.playerIndex)
            return KickoffType.CENTER
        }

        if (getNumberDistance(CHEATER_KICKOFF_X, xPosition) < WIGGLE_ROOM && getNumberDistance(CHEATER_KICKOFF_Y, yPosition) < WIGGLE_ROOM) {
            BotLog.println("it be cheatin", car.playerIndex)
            return KickoffType.CHEATIN
        }

        if (getNumberDistance(DIAGONAL_KICKOFF_X, xPosition) < WIGGLE_ROOM && getNumberDistance(DIAGONAL_KICKOFF_Y, yPosition) < WIGGLE_ROOM) {
            BotLog.println("it be slanterd", car.playerIndex)
            return KickoffType.SLANTERD
        }

        BotLog.println("what on earth", car.playerIndex)
        return KickoffType.UNKNOWN
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    companion object {
        private val DIAGONAL_KICKOFF_X = 40.98
        private val DIAGONAL_KICKOFF_Y = 40.98
        private val CHEATER_KICKOFF_X = 5.09
        private val CHEATER_KICKOFF_Y = 76.8
        private val CENTER_KICKOFF_X = 0.0
        private val CENTER_KICKOFF_Y = 92.16
        private val WIGGLE_ROOM = 2.0
        private val CHEATIN_BOOST_Y = 58.0

        private fun getNumberDistance(first: Double, second: Double): Double {
            return Math.abs(first - second)
        }
    }
}
