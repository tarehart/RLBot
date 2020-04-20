package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BotMath.numberDistance
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.KickoffAdvice
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import kotlin.math.abs
import kotlin.math.sign

class GoForKickoffStep(val dodgeDistance:Double = 20.0, val counterAttack: Boolean = false) : NestedPlanStep() {
    override fun getLocalSituation(): String {
        return "Going for kickoff"
    }

    private var kickoffType: KickoffType? = null
    private lateinit var startTime: GameTime

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        return !bundle.tacticalSituation.goForKickoff
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        if (bundle.agentInput.ballPosition.flatten().magnitudeSquared() > 0) {
            return null
        }

        val car = bundle.agentInput.myCarData

        if (bundle.tacticalSituation.ballAdvantage.seconds > 1) {
            BotLog.println("Opponent is not going for kickoff, will avoid rolling it to them.", car.playerIndex)
            return startPlan(Plan().withStep(FlexibleKickStep(KickAwayFromOpponent())), bundle)
        }

        if (kickoffType == null) {
            kickoffType = getKickoffType(car)
            startTime = bundle.agentInput.time
        }

        if (counterAttack && bundle.agentInput.matchInfo.isKickoffPause) {
            val goalLine = GoalUtil.getOwnGoal(bundle.agentInput.team).center.flatten()
            val toEnemy = goalLine.scaled(-1F)
            return startPlan(Plan(Posture.NEUTRAL)
                    .withStep(ParkTheCarStep { _ -> PositionFacing(goalLine, toEnemy) })
                    .withStep(BlindStep(Duration.ofSeconds(0.5), AgentOutput())),
                    bundle)
        }

        val target: Vector2
        val ySide = sign(car.position.y)
        if ((kickoffType == KickoffType.CHEATIN_LEFT || kickoffType == KickoffType.CHEATIN_RIGHT) && abs(car.position.y) > CHEATIN_BOOST_Y + 10) {
            // Steer toward boost
            target = Vector2(0.0, ySide * CHEATIN_BOOST_Y)

        } else {
            target = Vector2(0.0, ySide * 10)
        }

        if (kickoffType == KickoffType.SPACE_JAM) {
            return startPlan(Plan(Posture.NEUTRAL)
                    .withStep(BlindStep(Duration.ofSeconds(0.2), AgentOutput().withBoost().withThrottle(1.0)))
                    .withStep(MidairStrikeStep(Duration.ofMillis(0))), bundle)
        }

        if (bundle.agentInput.ballPosition.z > 3) {
            return startPlan(Plan(Posture.NEUTRAL).withStep(InterceptStep()), bundle)
        }

        if (car.position.magnitude() < dodgeDistance) {
            return startPlan(SetPieces.anyDirectionFlip(car, car.position.flatten().scaledToMagnitude(-1.0)), bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, target, detourForBoost = false)
    }

    enum class KickoffType constructor(val rank: Int) {
        CENTER(5),
        CHEATIN_RIGHT(4),
        CHEATIN_LEFT(3),
        SLANTERD_RIGHT(2),
        SLANTERD_LEFT(1),
        SPACE_JAM(10),
        UNKNOWN(100)
    }

    companion object {
        private val DIAGONAL_KICKOFF_X = 40.96F
        private val DIAGONAL_KICKOFF_Y = 51.2F
        private val CHEATER_KICKOFF_X = 5.12F
        private val CHEATER_KICKOFF_Y = 76.8F
        private val CENTER_KICKOFF_X = 0.0F
        private val CENTER_KICKOFF_Y = 92.16F
        private val WIGGLE_ROOM = 2.0F
        private val CHEATIN_BOOST_Y = 58.0F

        fun getKickoffType(car: CarData): KickoffType {
            val xPosition = abs(car.position.x)
            val yPosition = abs(car.position.y)
            if (numberDistance(CENTER_KICKOFF_X, xPosition) < WIGGLE_ROOM && numberDistance(CENTER_KICKOFF_Y, yPosition) < WIGGLE_ROOM) {
                BotLog.println("it be center", car.playerIndex)
                return KickoffType.CENTER
            }

            if (numberDistance(CHEATER_KICKOFF_X, xPosition) < WIGGLE_ROOM && numberDistance(CHEATER_KICKOFF_Y, yPosition) < WIGGLE_ROOM) {
                BotLog.println("it be cheatin", car.playerIndex)
                return if (xPosition * car.team.side > 0) KickoffType.CHEATIN_LEFT else KickoffType.CHEATIN_RIGHT
            }

            if (numberDistance(DIAGONAL_KICKOFF_X, xPosition) < WIGGLE_ROOM && numberDistance(DIAGONAL_KICKOFF_Y, yPosition) < WIGGLE_ROOM) {
                BotLog.println("it be slanterd", car.playerIndex)
                return if (xPosition * car.team.side > 0) KickoffType.SLANTERD_LEFT else KickoffType.SLANTERD_RIGHT
            }

            if (ArenaModel.isMicroGravity()) {
                return KickoffType.SPACE_JAM
            }

            BotLog.println("what on earth", car.playerIndex)
            return KickoffType.UNKNOWN
        }

        fun chooseKickoffPlan(bundle: TacticalBundle, kickoffAdvice: KickoffAdvice): Plan {
            val car = bundle.agentInput.myCarData
            val kickoffPriorities = bundle.agentInput.getTeamRoster(car.team).sortedBy { GoForKickoffStep.getKickoffType(it).rank }

            if (kickoffPriorities.first() == car) {
                return Plan(Posture.KICKOFF).withStep(GoForKickoffStep(
                        dodgeDistance = kickoffAdvice.dodgeRange,
                        counterAttack = kickoffAdvice.counterAttack))
            }

            if (kickoffPriorities.last() == car) {
                val expiryTime = car.time.plusSeconds(3)
                return RetryableViableStepPlan(Posture.KICKOFF, "Covering goal on kickoff as last back", GetOnDefenseStep()) { b -> b.agentInput.time < expiryTime }
            }

            return Plan(Posture.KICKOFF, "Getting boost during kickoff").withStep(GetBoostStep())
        }
    }
}
