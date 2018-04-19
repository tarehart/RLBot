package tarehart.rlbot.input

import tarehart.rlbot.math.vector.Vector3

class CarOrientation(val noseVector: Vector3, val roofVector: Vector3) {
    val rightVector: Vector3 = noseVector.crossProduct(roofVector)
}
