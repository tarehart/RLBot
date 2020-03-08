package tarehart.rlbot.math.vector



class Spin(val pitchRate: Float, val yawRate: Float, val rollRate:Float) {

    constructor(angularVelLocal: Vector3): this(-angularVelLocal.y, -angularVelLocal.z, angularVelLocal.x)
}
