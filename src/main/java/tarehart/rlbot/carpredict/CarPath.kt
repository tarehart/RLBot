package tarehart.rlbot.carpredict

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color
import java.util.ArrayList

class CarPath {

    val path = ArrayList<CarSlice>()

    val firstSlice
        get() = path[0]

    val lastSlice
        get() = path[path.size - 1]

    fun addSlice(slice: CarSlice) {
        path.add(slice)
    }

    fun renderIn3d(renderer: BotLoopRenderer) {
        RenderUtil.drawPath(renderer, path.map { it.space }, Color.CYAN)
    }

    fun getFirstPlaneBreak(planes: List<Plane>): Impact? {
        for (i in 1 until this.path.size) {
            val currSlice = this.path[i]

            val prevSlice = this.path[i - 1]

            for (p: Plane in planes) {
                getPlaneBreak(prevSlice.space, currSlice.space, p)?.let {
                    return Impact(it, p.normal)
                }
            }
        }

        return null
    }

    private fun getPlaneBreak(start: Vector3, end: Vector3, plane: Plane): Vector3? {
        return VectorUtil.getPlaneIntersection(plane, start, end.minus(start))
    }

}
