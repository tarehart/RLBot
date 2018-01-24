package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tuning.BotLog
import java.awt.Graphics2D
import java.util.*

class GoForKickoffStep : NestedPlanStep() {
    override fun getLocalSituation(): String {
        return "Going for kickoff"
    }

    private var kickoffType: KickoffType? = null

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {
        if (input.ballPosition.flatten().magnitudeSquared() > 0) {
            return null
        }

        val car = input.myCarData

        if (kickoffType == null) {
            kickoffType = getKickoffType(car)
        }

        val distance = car.position.magnitude()
        if (distance < 14) {
            startPlan(SetPieces.frontFlip(), input)
        }

        val ySide = Math.signum(car.position.y)

        val target: Vector2
        if (kickoffType == KickoffType.CHEATIN && Math.abs(car.position.y) > CHEATIN_BOOST_Y + 10) {
            // Steer toward boost
            target = Vector2(0.0, ySide * CHEATIN_BOOST_Y)
        } else if (distance > 30) {
            target = Vector2(0.0, ySide * 15)
        } else {
            target = Vector2(0.0, 0.0)
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

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
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
