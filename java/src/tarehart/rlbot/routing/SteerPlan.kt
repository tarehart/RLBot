package tarehart.rlbot.routing

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D

class SteerPlan @JvmOverloads constructor(val immediateSteer: AgentOutput, val waypoint: Vector2, val strikePoint: StrikePoint, val circle: Circle? = null, val clockwise: Boolean = false) {

    fun drawDebugInfo(graphics: Graphics2D, car: CarData) {

        graphics.stroke = BasicStroke(1f)
        graphics.draw(Line2D.Double(car.position.x, car.position.y, waypoint.x, waypoint.y))
        if (circle != null) {
            val circleShape = circle.toShape()


            //graphics.draw(circleShape);

            if (waypoint.distance(strikePoint.position) > .01) {

                val centerToWaypoint = waypoint.minus(circle.center)
                val centerToFinal = strikePoint.position.minus(circle.center)
                val waypointAngle = Math.atan2(centerToWaypoint.y, centerToWaypoint.x)
                val waypointDegrees = -waypointAngle * 180 / Math.PI
                val radians = centerToWaypoint.correctionAngle(centerToFinal, clockwise)
                val extent = -radians * 180 / Math.PI

                val arc = Arc2D.Double(circleShape.bounds2D, waypointDegrees, extent, Arc2D.OPEN)
                graphics.draw(arc)
            }

            graphics.draw(Circle(waypoint, 1.0).toShape())

            strikePoint.positionFacing.drawDebugInfo(graphics)
        }

    }
}
