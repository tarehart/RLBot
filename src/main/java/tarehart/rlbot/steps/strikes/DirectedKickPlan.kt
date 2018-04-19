package tarehart.rlbot.steps.strikes

import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.routing.StrikePoint
import tarehart.rlbot.ui.ArenaDisplay
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class DirectedKickPlan (
    val intercept: Intercept,
    val ballPath: BallPath,
    val distancePlot: DistancePlot,
    val ballAtIntercept: BallSlice,
    val interceptModifier: Vector3,
    val desiredBallVelocity: Vector3,
    val plannedKickForce: Vector3,
    val launchPad: StrikePoint,
    val easyKickAllowed: Boolean
) {

    fun drawDebugInfo(graphics: Graphics2D) {
        graphics.color = Color(73, 111, 73)
        ArenaDisplay.drawBall(ballAtIntercept.space, graphics, graphics.color)
        graphics.stroke = BasicStroke(1f)

        val (x, y) = intercept.space.flatten()
        val crossSize = 2
        graphics.draw(Line2D.Double(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
        graphics.draw(Line2D.Double(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
    }
}
