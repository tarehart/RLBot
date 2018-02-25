package tarehart.rlbot.steps.rotation

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3

class RollToPlaneStep(planeNormal: Vector3, allowUpsideDown: Boolean = false) : OrientToPlaneStep({planeNormal}, allowUpsideDown) {

    override val spinDeceleration: Double = 80.0

    override val situation = "Rolling in midair"

    override fun getOrientationCorrection(car: CarData): Double {
        val vectorNeedingCorrection = car.orientation.rightVector
        val axisOfRotation = car.orientation.noseVector

        // Negate the correction radians. If the right vector is above the plane, the function will indicate a negative
        // correction, but we need to roll right which is considered the positive direction.
        var correction = -getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection, axisOfRotation)

        val upsideDown = car.orientation.roofVector.dotProduct(planeNormal) < 0

        if (upsideDown) {
            correction *= -1.0 // When upside down, need to rotate the opposite direction to converge on plane.
            if (!allowUpsideDown) {
                correction += Math.PI // Turn all the way around
            }
        }

        return RotationUtil.shortWay(correction)
    }

    override fun getAngularVelocity(car: CarData): Double {
        return car.spin.rollRate
    }

    override fun accelerate(positiveRadians: Boolean): AgentOutput {
        return AgentOutput().withRoll((if (positiveRadians) 1 else -1).toDouble())
    }
}
