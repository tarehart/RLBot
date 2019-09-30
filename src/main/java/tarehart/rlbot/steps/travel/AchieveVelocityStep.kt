package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.StandardStep

class AchieveVelocityStep(private val velocity: Vector3) : StandardStep() {

    override val situation: String
        get() = "AchievingVelocity"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (!car.hasWheelContact) {
            return null
        }

        val carVelOnPlane = car.velocity.projectToPlane(car.orientation.roofVector)
        val targetVelOnPlane = velocity.projectToPlane(car.orientation.roofVector)

        val alignment = carVelOnPlane.normaliseCopy().dotProduct(targetVelOnPlane.normaliseCopy())
        if (alignment  > .9 && carVelOnPlane.distance(targetVelOnPlane) < 5) {
            if (velocity.dotProduct(car.orientation.roofVector) > 1) {
                return AgentOutput().withJump()
            }
            return null
        }

        val steer = SteerUtil.steerTowardWallPosition(car, car.position + velocity)
        if (car.orientation.noseVector.dotProduct(targetVelOnPlane.normaliseCopy()) < -.5) {
            steer.withThrottle(-1.0).withBoost(false)
        }

        if (alignment > .9 && carVelOnPlane.magnitudeSquared() > targetVelOnPlane.magnitudeSquared()) {
            val brakingThrottle = if (car.velocity.dotProduct(car.orientation.noseVector) > 0) -1.0 else 1.0
            steer.withThrottle(brakingThrottle).withBoost(false)
        }

        return steer
    }
}
