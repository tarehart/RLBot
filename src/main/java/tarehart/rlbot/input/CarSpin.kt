package tarehart.rlbot.input

import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.vector.Spin
import tarehart.rlbot.math.vector.Vector3

/**
 * All values are in radians per second.
 */
class CarSpin(val spin: Spin, val angularVel: Vector3) {

    constructor(pitchRate: Double, yawRate: Double, rollRate: Double, angularVel: Vector3):
            this(Spin(pitchRate, yawRate, rollRate), angularVel)

    constructor(angularVel: Vector3, orientation: Mat3): this(Spin(orientation.transpose().dot(angularVel)), angularVel)

    val pitchRate = spin.pitchRate
    val yawRate = spin.yawRate
    val rollRate = spin.rollRate
}
