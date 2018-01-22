package tarehart.rlbot.physics

object BallPhysics {
    fun getGroundBounceEnergy(height: Double, verticalVelocity: Double): Double {
        val potentialEnergy = (height - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY
        val verticalKineticEnergy = 0.5 * verticalVelocity * verticalVelocity
        return potentialEnergy + verticalKineticEnergy
    }
}
