package tarehart.rlbot.carpredict

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.rendering.RenderUtil
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
}
