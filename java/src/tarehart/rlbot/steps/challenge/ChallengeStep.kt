package tarehart.rlbot.steps.challenge

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.*
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticsTelemetry
import tarehart.rlbot.steps.ChaseBallStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.util.*

class ChallengeStep: NestedPlanStep() {

    override fun getLocalSituation(): String {
        return  "Working on challenge"
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {

        val carData = input.myCarData

        return startPlan(Plan().withStep(ChaseBallStep()), input)


    }
}
