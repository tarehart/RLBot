package tarehart.rlbot.rendering

import rlbot.manager.BotLoopRenderer
import rlbot.render.Renderer
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.GameTime
import java.awt.Color


object RenderUtil {
    val STANDARD_BALL_PATH_COLOR: Color = Color.YELLOW

    fun drawSquare(renderer: Renderer, plane: Plane, extent: Double, color: Color) {

        val crosser = if (plane.normal.x == 0.0 && plane.normal.y == 0.0) Vector3(1.0, 0.0, 0.0) else Vector3.UP
        val majorAxisNormalized = plane.normal.crossProduct(crosser)
        val minorAxis = plane.normal.crossProduct(majorAxisNormalized).scaled(extent)
        val majorAxis = majorAxisNormalized.scaled(extent)

        val p1 = plane.position + majorAxis + minorAxis
        val p2 = plane.position + majorAxis - minorAxis
        val p3 = plane.position - majorAxis - minorAxis
        val p4 = plane.position - majorAxis + minorAxis

        renderer.drawLine3d(color, p1.toRlbot(), p2.toRlbot())
        renderer.drawLine3d(color, p2.toRlbot(), p3.toRlbot())
        renderer.drawLine3d(color, p3.toRlbot(), p4.toRlbot())
        renderer.drawLine3d(color, p4.toRlbot(), p1.toRlbot())
    }

    fun drawImpact(renderer: Renderer, tipPosition: Vector3, force: Vector3, color: Color) {

        val extent = force.magnitude() / 8
        val crosser = if (force.x == 0.0 && force.y == 0.0) Vector3(1.0, 0.0, 0.0) else Vector3.UP
        val forceNormal = force.normaliseCopy()
        val majorAxisNormalized = forceNormal.crossProduct(crosser)
        val minorAxis = forceNormal.crossProduct(majorAxisNormalized).scaled(extent)
        val majorAxis = majorAxisNormalized.scaled(extent)

        val p1 = tipPosition - force + majorAxis + minorAxis
        val p2 = tipPosition - force + majorAxis - minorAxis
        val p3 = tipPosition - force - majorAxis - minorAxis
        val p4 = tipPosition - force - majorAxis + minorAxis

        renderer.drawLine3d(color, p1.toRlbot(),tipPosition.toRlbot())
        renderer.drawLine3d(color, p2.toRlbot(),tipPosition.toRlbot())
        renderer.drawLine3d(color, p3.toRlbot(),tipPosition.toRlbot())
        renderer.drawLine3d(color, p4.toRlbot(),tipPosition.toRlbot())
    }

    fun drawBallPath(renderer: Renderer, ballPath: BallPath, endTime: GameTime, color: Color) {
        var prevSlice = ballPath.startPoint
        var slice: BallSlice
        var i = 1
        while (prevSlice.time < endTime && i < ballPath.slices.size) {
            slice = ballPath.slices[i]

            renderer.drawLine3d(color, prevSlice.space.toRlbot(), slice.space.toRlbot())
            i += 1
            prevSlice = slice
        }
    }

    fun drawSphere(renderer: Renderer, position: Vector3, radius: Double, color: Color) {
        val x = Vector3(radius, 0.0, 0.0)
        val y = Vector3(0.0, radius, 0.0)
        val z = Vector3(0.0, 0.0, radius)

        val diag = Vector3(1.0, 1.0, 1.0).scaledToMagnitude(radius).x
        val d1 = Vector3(diag, diag, diag)
        val d2 = Vector3(-diag, diag, diag)
        val d3 = Vector3(-diag, -diag, diag)
        val d4 = Vector3(-diag, diag, -diag)

        renderer.drawLine3d(color, (position - x).toRlbot(), (position + x).toRlbot())
        renderer.drawLine3d(color, (position - y).toRlbot(), (position + y).toRlbot())
        renderer.drawLine3d(color, (position - z).toRlbot(), (position + z).toRlbot())
        renderer.drawLine3d(color, (position - d1).toRlbot(), (position + d1).toRlbot())
        renderer.drawLine3d(color, (position - d2).toRlbot(), (position + d2).toRlbot())
        renderer.drawLine3d(color, (position - d3).toRlbot(), (position + d3).toRlbot())
        renderer.drawLine3d(color, (position - d4).toRlbot(), (position + d4).toRlbot())
    }

}