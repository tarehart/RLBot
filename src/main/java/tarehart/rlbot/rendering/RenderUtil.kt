package tarehart.rlbot.rendering

import rlbot.render.Renderer
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.ui.DisplayFlags
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min


object RenderUtil {
    val STANDARD_BALL_PATH_COLOR: Color = Color.YELLOW

    fun drawSquare(renderer: Renderer, plane: Plane, extent: Number, color: Color) {

        val crosser = if (plane.normal.x == 0F && plane.normal.y == 0F) Vector3(1.0, 0.0, 0.0) else Vector3.UP
        val majorAxisNormalized = plane.normal.crossProduct(crosser)
        val minorAxis = plane.normal.crossProduct(majorAxisNormalized).scaled(extent.toFloat())
        val majorAxis = majorAxisNormalized.scaled(extent.toFloat())

        val p1 = plane.position + majorAxis + minorAxis
        val p2 = plane.position + majorAxis - minorAxis
        val p3 = plane.position - majorAxis - minorAxis
        val p4 = plane.position - majorAxis + minorAxis

        renderer.drawLine3d(color, p1, p2)
        renderer.drawLine3d(color, p2, p3)
        renderer.drawLine3d(color, p3, p4)
        renderer.drawLine3d(color, p4, p1)
    }

    fun drawImpact(renderer: Renderer, tipPosition: Vector3, force: Vector3, color: Color) {

        val extent = force.magnitude() / 8
        val crosser = if (force.x == 0F && force.y == 0F) Vector3(1.0, 0.0, 0.0) else Vector3.UP
        val forceNormal = force.normaliseCopy()
        val majorAxisNormalized = forceNormal.crossProduct(crosser)
        val minorAxis = forceNormal.crossProduct(majorAxisNormalized).scaled(extent)
        val majorAxis = majorAxisNormalized.scaled(extent)

        val p1 = tipPosition - force + majorAxis + minorAxis
        val p2 = tipPosition - force + majorAxis - minorAxis
        val p3 = tipPosition - force - majorAxis - minorAxis
        val p4 = tipPosition - force - majorAxis + minorAxis

        renderer.drawLine3d(color, p1, tipPosition)
        renderer.drawLine3d(color, p2, tipPosition)
        renderer.drawLine3d(color, p3, tipPosition)
        renderer.drawLine3d(color, p4, tipPosition)
    }

    fun drawBallPath(renderer: Renderer, ballPath: BallPath, endTime: GameTime, color: Color) {
        if(DisplayFlags[DisplayFlags.BALL_PATH] == 1) {
            var prevSlice = ballPath.startPoint
            var slice: BallSlice
            var i = 1
            while (prevSlice.time < endTime && i < ballPath.slices.size) {
                slice = ballPath.slices[i]

                renderer.drawLine3d(color, prevSlice.space, slice.space)
                i += 1
                prevSlice = slice
            }
        }
    }

    fun drawPath(renderer: Renderer, points: List<Vector3>, color: Color, step: Int = 1) {
        if (points.size < 2) {
            return
        }

        if(DisplayFlags[DisplayFlags.CAR_PATH] == 1) {
            for (i in step until points.size step step) {
                renderer.drawLine3d(color, points[i - step], points[i])
            }
        }
    }

    fun drawSphere(renderer: Renderer, position: Vector3, radius: Number, color: Color) {
        val x = Vector3(radius, 0.0, 0.0)
        val y = Vector3(0.0, radius, 0.0)
        val z = Vector3(0.0, 0.0, radius)

        val diag = Vector3(1.0, 1.0, 1.0).scaledToMagnitude(radius).x
        val d1 = Vector3(diag, diag, diag)
        val d2 = Vector3(-diag, diag, diag)
        val d3 = Vector3(-diag, -diag, diag)
        val d4 = Vector3(-diag, diag, -diag)

        renderer.drawLine3d(color, position - x, position + x)
        renderer.drawLine3d(color, position - y, position + y)
        renderer.drawLine3d(color, position - z, position + z)
        renderer.drawLine3d(color, position - d1, position + d1)
        renderer.drawLine3d(color, position - d2, position + d2)
        renderer.drawLine3d(color, position - d3, position + d3)
        renderer.drawLine3d(color, position - d4, position + d4)
    }

    fun drawCircle(renderer: Renderer, circle: Circle, height: Number, color: Color) {
        return drawRadarChart(renderer, circle.center, 16, height, color) { circle.radius }
    }

    fun drawRadarChart(renderer: Renderer, center: Vector2, numSegments: Int, height: Number, color: Color, radiusFunction: (Number) -> Float) {
        var radians = 0.0
        var cursor = Vector2(max(0.001F, radiusFunction(radians)), 0.0)
        val increment = 2 * Math.PI.toFloat() / numSegments

        while (radians < Math.PI * 2) {
            radians += increment
            val nextCursor = VectorUtil.rotateVector(cursor, increment).scaledToMagnitude(max(0.001F, radiusFunction(radians)))

            val current = center + cursor
            val next = center + nextCursor
            if (!(current - next).isZero) {
                renderer.drawLine3d(color, current.withZ(height), next.withZ(height))
            }
            cursor = nextCursor
        }
    }

    fun drawSphereOfInfluence(car: CarData, distancePlot: DistancePlot, duration: Duration) {
        drawRadarChart(car.renderer, car.position.flatten(), 64, car.position.z, Color.GREEN) {
            val direction = VectorUtil.rotateVector(Vector2(1, 0), it.toFloat())
            distancePlot.getMaximumRange(car, direction.withZ(0), duration) ?: 0F
        }
    }

    fun drawSphereSlice(renderer: Renderer, center: Vector3, radius: Float, sliceHeight: Float, color: Color) {

        val slice = Circle.getCircleFromSphereSlice(center, radius, sliceHeight) ?: return

        var cursor = Vector2(slice.radius, 0.0)
        var radians = 0.0
        val flatCenter = center.flatten()

        while (radians < Math.PI * 2) {
            radians += Math.PI / 8
            val nextCursor = VectorUtil.rotateVector(cursor, Math.PI.toFloat() / 8)
            renderer.drawLine3d(color, (flatCenter + cursor).withZ(sliceHeight), (flatCenter + nextCursor).withZ(sliceHeight))
            cursor = nextCursor
        }
    }

    private val RAND = Random()

    fun randomColor(): Color {
        return Color.getHSBColor(RAND.nextFloat(), 1F, 0.7F)
    }

    private val RAINBOW_COLORS = listOf(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA)
    fun rainbowColor(index: Int): Color {
        return RAINBOW_COLORS[index % RAINBOW_COLORS.size]
    }

}
