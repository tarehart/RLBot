package tarehart.rlbot.math.vector



class Spin(val pitchRate: Double, val yawRate: Double, val rollRate:Double) {

    constructor(angularVelLocal: Vector3): this(-angularVelLocal.y, -angularVelLocal.z, angularVelLocal.x)
}
