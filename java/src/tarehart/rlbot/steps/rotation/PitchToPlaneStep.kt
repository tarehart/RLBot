package tarehart.rlbot.steps.rotation

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3

class PitchToPlaneStep(planeNormal: Vector3, allowUpsideDown: Boolean = false) : OrientToPlaneStep({planeNormal}, allowUpsideDown) {

    override val spinDeceleration: Double = 6.0

    override val situation = "Pitching in midair"


    override fun getOrientationCorrection(car: CarData): Double {
        val vectorNeedingCorrection = car.orientation.noseVector
        val axisOfRotation = car.orientation.rightVector
        var correction = getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection, axisOfRotation)

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
        return car.spin.pitchRate
    }

    override fun accelerate(positiveRadians: Boolean): AgentOutput {
        return AgentOutput().withPitch((if (positiveRadians) 1 else -1).toDouble())
    }

}
