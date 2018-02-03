package tarehart.rlbot.steps.rotation

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3

import java.util.function.Function

class YawToPlaneStep(planeNormalFn: (AgentInput) -> Vector3, allowUpsideDown: Boolean = false) : OrientToPlaneStep(planeNormalFn, allowUpsideDown) {

    override val spinDeceleration: Double = 6.0

    override val situation = "Yawing in midair"

    override fun getOrientationCorrection(car: CarData): Double {
        val vectorNeedingCorrection = car.orientation.noseVector
        val axisOfRotation = car.orientation.roofVector
        var correction = getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection, axisOfRotation)

        val wrongDirection = car.orientation.rightVector.dotProduct(planeNormal) < 0

        if (wrongDirection) {
            correction *= -1.0 // When upside down, need to rotate the opposite direction to converge on plane.
            if (!allowUpsideDown) {
                correction += Math.PI // Turn all the way around
            }
        }
        return RotationUtil.shortWay(correction)
    }

    override fun getAngularVelocity(car: CarData): Double {
        return car.spin.yawRate
    }

    override fun accelerate(positiveRadians: Boolean): AgentOutput {
        return AgentOutput().withYaw((if (positiveRadians) 1 else -1).toDouble())
    }
}
