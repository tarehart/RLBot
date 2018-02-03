package tarehart.rlbot.steps.rotation

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.steps.Step

import java.awt.*


abstract class OrientToPlaneStep(private val planeNormalFn: (AgentInput) -> Vector3, protected var allowUpsideDown: Boolean) : Step {
    protected lateinit var planeNormal: Vector3
    private var timeToDecelerate: Boolean = false
    private var originalCorrection: Double? = null

    protected abstract val spinDeceleration: Double

    private fun getRadiansSpentDecelerating(angularVelocity: Double): Double {
        val velocityMagnitude = Math.abs(angularVelocity)
        val spinDeceleration = spinDeceleration
        val timeDecelerating = velocityMagnitude / spinDeceleration
        return velocityMagnitude * timeDecelerating - .5 * spinDeceleration * timeDecelerating * timeDecelerating
    }

    /**
     * This does not consider direction. You should only call it if you are already rotating toward your target.
     */
    protected fun timeToDecelerate(angularVelocity: Double, radiansRemaining: Double): Boolean {
        return getRadiansSpentDecelerating(angularVelocity) >= Math.abs(radiansRemaining)
    }

    /**
     * Does not care if we go the "wrong way" and end up upside down.
     */
    protected fun getMinimalCorrectionRadiansToPlane(vectorNeedingCorrection: Vector3, axisOfRotation: Vector3): Double {
        // We want vectorNeedingCorrection to be resting on the plane. If it's lined up with the planeNormal, then it's
        // doing a very poor job of that.
        val planeError = VectorUtil.project(vectorNeedingCorrection, planeNormal)

        val distanceAbovePlane = planeError.magnitude() * Math.signum(planeError.dotProduct(planeNormal))

        val maxOrbitHeightAbovePlane = RotationUtil.maxOrbitHeightAbovePlane(axisOfRotation, planeNormal)
        return -Math.asin(distanceAbovePlane / maxOrbitHeightAbovePlane)
    }

    protected abstract fun getOrientationCorrection(car: CarData): Double

    protected abstract fun getAngularVelocity(car: CarData): Double

    protected abstract fun accelerate(positiveRadians: Boolean): AgentOutput

    private fun accelerateTowardPlane(car: CarData): AgentOutput {

        val correctionRadians = getOrientationCorrection(car)

        val angularVelocity = getAngularVelocity(car)

        if (angularVelocity * correctionRadians > 0) {
            // We're trending toward the plane, that's good.
            if (timeToDecelerate(angularVelocity, correctionRadians)) {
                timeToDecelerate = true
            }
        }

        return accelerate(correctionRadians > 0)
    }

    override fun getOutput(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        if (car.hasWheelContact) {
            return null
        }

        planeNormal = planeNormalFn.invoke(input)

        if (originalCorrection == null) {
            originalCorrection = getOrientationCorrection(car)
        }

        var output = AgentOutput()
        if (!timeToDecelerate) {
            output = accelerateTowardPlane(input.myCarData)
        }

        // The value of timeToDecelerate can get changed by accelerateTowardPlane.
        if (timeToDecelerate) {
            if (getAngularVelocity(car) * originalCorrection!! < 0) {
                // We're done decelerating
                return null
            }

            output = accelerate(originalCorrection!! < 0)
        }

        output.withThrottle(1.0) // Just in case we're stuck on our side on the ground

        return output
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
