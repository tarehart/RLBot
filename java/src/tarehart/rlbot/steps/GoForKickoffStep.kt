package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.strikes.DirectedNoseHitStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import java.awt.Graphics2D
import java.util.*

class GoForKickoffStep : NestedPlanStep() {
    override fun getLocalSituation(): String {
        return "Going for kickoff"
    }

    private var kickoffType: KickoffType? = null
    private var counterAttack = Math.random() < .3
    private lateinit var startTime: GameTime

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {
        if (input.ballPosition.flatten().magnitudeSquared() > 0) {
            return null
        }

        val car = input.myCarData

        if (kickoffType == null) {
            kickoffType = getKickoffType(car)
            startTime = input.time
        }

        if ((kickoffType == KickoffType.CENTER || kickoffType == KickoffType.CHEATIN) && counterAttack) {
            if ((input.time - startTime).seconds < 8) {
                return null
            }
            return AgentOutput() // Wait for them to hit it, then counter attack
        }

        val target: Vector2
        if (kickoffType == KickoffType.CHEATIN && Math.abs(car.position.y) > CHEATIN_BOOST_Y + 10) {
            // Steer toward boost
            val ySide = Math.signum(car.position.y)
            target = Vector2(0.0, ySide * CHEATIN_BOOST_Y)
        } else {
            return startPlan(FirstViableStepPlan(Plan.Posture.NEUTRAL).withStep(ChallengeStep()), input)
        }
        return SteerUtil.steerTowardGroundPosition(car, target)
    }

    private enum class KickoffType {
        CENTER,
        CHEATIN,
        SLANTERD,
        UNKNOWN
    }

    private fun getKickoffType(car: CarData): KickoffType {
        val xPosition = car.position.x
        if (getNumberDistance(CENTER_KICKOFF_X, xPosition) < WIGGLE_ROOM) {
            BotLog.println("it be center", car.playerIndex)
            return KickoffType.CENTER
        }

        if (getNumberDistance(CHEATER_KICKOFF_X, Math.abs(xPosition)) < WIGGLE_ROOM) {
            BotLog.println("it be cheatin", car.playerIndex)
            return KickoffType.CHEATIN
        }

        if (getNumberDistance(DIAGONAL_KICKOFF_X, Math.abs(xPosition)) < WIGGLE_ROOM) {
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
        private val CHEATER_KICKOFF_X = 5.09
        private val CENTER_KICKOFF_X = 0.0
        private val WIGGLE_ROOM = 2.0
        private val CHEATIN_BOOST_Y = 58.0

        private fun getNumberDistance(first: Double, second: Double): Double {
            return Math.abs(first - second)
        }
    }
}
