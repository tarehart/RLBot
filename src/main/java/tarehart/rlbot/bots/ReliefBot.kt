package tarehart.rlbot.bots

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import java.awt.Color

class ReliefBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {

    override fun getOutput(bundle: TacticalBundle): AgentOutput {

        bundle.tacticalSituation.expectedContact?.let {
            val distancePlot = it.distancePlot
            val car = bundle.agentInput.myCarData

            RenderUtil.drawRadarChart(car.renderer, car.position.flatten(), 64, car.position.z, Color.CYAN) {
                val direction = VectorUtil.rotateVector(Vector2(1, 0), it)
                distancePlot.getMaximumRange(car, direction, Duration.ofSeconds(3.0)) ?: 0.0
            }

            RenderUtil.drawRadarChart(car.renderer, car.position.flatten(), 64, car.position.z, Color.GREEN) {
                val direction = VectorUtil.rotateVector(Vector2(1, 0), it)
                distancePlot.getMaximumRange(car, direction, Duration.ofSeconds(1.0)) ?: 0.0
            }

            RenderUtil.drawRadarChart(car.renderer, car.position.flatten(), 64, car.position.z, Color.YELLOW) {
                val direction = VectorUtil.rotateVector(Vector2(1, 0), it)
                distancePlot.getMaximumRange(car, direction, Duration.ofSeconds(0.5)) ?: 0.0
            }
        }

        return super.getOutput(bundle)
    }
}
