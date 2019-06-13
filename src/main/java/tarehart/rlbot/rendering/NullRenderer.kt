package tarehart.rlbot.rendering

import rlbot.render.Renderer
import rlbot.vector.Vector3
import java.awt.Color
import java.awt.Point

class NullRenderer: Renderer(0) {
    override fun drawString3d(text: String?, color: Color?, upperLeft: Vector3?, scaleX: Int, scaleY: Int) {}

    override fun hasContent(): Boolean {
        return false
    }

    override fun drawString2d(text: String?, color: Color?, upperLeft: Point?, scaleX: Int, scaleY: Int) {}

    override fun drawCenteredRectangle3d(color: Color?, position: Vector3?, width: Int, height: Int, filled: Boolean) {}

    override fun drawRectangle3d(color: Color?, upperLeft: Vector3?, width: Int, height: Int, filled: Boolean) {}

    override fun drawRectangle2d(color: Color?, upperLeft: Point?, width: Int, height: Int, filled: Boolean) {}

    override fun resetPacket() {}

    override fun drawLine3d(color: Color?, start: Vector3?, end: Vector3?) {}

    override fun drawLine2d(color: Color?, start: Point?, end: Point?) {}

    override fun drawLine2d3d(color: Color?, start: Point?, end: Vector3?) {}

    override fun eraseFromScreen() {}
}
