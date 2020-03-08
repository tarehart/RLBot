package tarehart.rlbot.carpredict

import rlbot.render.Renderer
import tarehart.rlbot.input.CarData
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime
import java.awt.Color

data class CarSlice(
        val space: Vector3,
        val time: GameTime,
        val velocity: Vector3,
        val orientation: CarOrientation,
        val hitbox: CarHitbox = CarHitbox.OCTANE) {

    constructor(carData: CarData): this(carData.position, carData.time, carData.velocity, carData.orientation, carData.hitbox)

    fun toSpaceTime(): SpaceTime {
        return SpaceTime(space, time)
    }

    fun render(renderer: Renderer, color: Color) {
        val toRoof = orientation.roofVector * (hitbox.height / 2)
        val toNose = orientation.noseVector * (hitbox.length / 2)
        val toSide = orientation.rightVector * (hitbox.width / 2)

        val localOffset = orientation.matrix.dot(hitbox.offset)
        val overallOffset = space + localOffset

        renderer.drawLine3d(color, toRoof + toNose + toSide + overallOffset, toRoof + toNose - toSide + overallOffset)
        renderer.drawLine3d(color, toRoof + toNose + toSide + overallOffset, toRoof - toNose + toSide + overallOffset)
        renderer.drawLine3d(color, toRoof + toNose - toSide + overallOffset, toRoof - toNose - toSide + overallOffset)
        renderer.drawLine3d(color, toRoof - toNose + toSide + overallOffset, toRoof - toNose - toSide + overallOffset)
    }
}
