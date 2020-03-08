package tarehart.rlbot.carpredict

import rlbot.render.Renderer
import tarehart.rlbot.input.CarData
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.math.Ray
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

    val hitboxCenterLocal: Vector3 by lazy { orientation.matrix.dot(hitbox.offset) }
    val hitboxCenterWorld: Vector3 by lazy { hitboxCenterLocal + space }

    // Caveat: these use world rotation frame, but car-local position with no offset!
    val toRoof: Vector3 by lazy { orientation.roofVector * (hitbox.height / 2) }
    val toNose: Vector3 by lazy { orientation.noseVector * (hitbox.length / 2) }
    val toSide: Vector3 by lazy { orientation.rightVector * (hitbox.width / 2) }

    fun render(renderer: Renderer, color: Color) {
        renderer.drawLine3d(color, toRoof + toNose + toSide + hitboxCenterWorld, toRoof + toNose - toSide + hitboxCenterWorld)
        renderer.drawLine3d(color, toRoof + toNose + toSide + hitboxCenterWorld, toRoof - toNose + toSide + hitboxCenterWorld)
        renderer.drawLine3d(color, toRoof + toNose - toSide + hitboxCenterWorld, toRoof - toNose - toSide + hitboxCenterWorld)
        renderer.drawLine3d(color, toRoof - toNose + toSide + hitboxCenterWorld, toRoof - toNose - toSide + hitboxCenterWorld)
    }

    /**
     * Left headlight is first.
     */
    fun headlightRays(): Pair<Ray, Ray> {
        return Pair(
                Ray(toRoof + toNose + toSide * -1 + hitboxCenterWorld, orientation.noseVector),
                Ray(toRoof + toNose + toSide + hitboxCenterWorld, orientation.noseVector))
    }
}
