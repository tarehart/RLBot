package tarehart.rlbot.physics

import tarehart.rlbot.math.vector.Vector3
import kotlin.math.min

object BallPhysics {

    val BALL_MASS = 30.0
    val CAR_MASS = 180.0

    val UU_CONST = Vector3.PACKET_DISTANCE_TO_CLASSIC
    val pushFactorCurve = mapOf(
            0    / UU_CONST to 0.65,
            500  / UU_CONST to 0.6,
            1400 / UU_CONST to 0.55,
            2300 / UU_CONST to 0.5,
            4600 / UU_CONST to 0.3)

    fun getGroundBounceEnergy(height: Double, verticalVelocity: Double): Double {
        val potentialEnergy = (height - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY
        val verticalKineticEnergy = 0.5 * verticalVelocity * verticalVelocity
        return potentialEnergy + verticalKineticEnergy
    }

    private fun ballPushFactorCurve(relativeSpeed: Double): Double {
        if (relativeSpeed in pushFactorCurve) {
            return pushFactorCurve[relativeSpeed]!!
        }
        val keys = pushFactorCurve.keys.map { it }
        for (i in 1..pushFactorCurve.size) {
            val lower = keys[i - 1]
            val higher = keys[i]
            if (lower < relativeSpeed && higher > relativeSpeed ) {
                val difference = higher - lower
                val relativeSpeedScale = (relativeSpeed - lower) / difference
                return pushFactorCurve[lower]!! + relativeSpeedScale * (pushFactorCurve[higher]!! - pushFactorCurve[lower]!!)
            }
        }
        return 0.65
    }

    // https://gist.github.com/nevercast/407cc224d5017622dbbd92e70f7c9823
    fun calculateScriptBallImpactForce(carPosition: Vector3,
                                       carVelocity: Vector3,
                                       ballPosition: Vector3,
                                       ballVelocity: Vector3,
                                       carForwardDirectionNormal: Vector3): Vector3 {
        val BallInteraction_MaxRelativeSpeed = 4600.0
        val BallInteraction_PushZScale = 0.35
        val BallInteraction_PushForwardScale = 0.65

        val RelVel = carVelocity - ballVelocity
        // UKismetMathLibrary::VSize
        var RelSpeed = RelVel.magnitude() // Equivalent to Vector3 Length, equal to VSize
        if (RelSpeed > 0.0) {
            RelSpeed = min(RelSpeed, BallInteraction_MaxRelativeSpeed)
            var HitDir = (ballPosition - carPosition)
            HitDir = HitDir.withZ(HitDir.z * BallInteraction_PushZScale)
            HitDir = HitDir.normaliseCopy()

            // If PushForwardScale != 1.0
            val ForwardDir = carForwardDirectionNormal
            val ForwardAdjustment = (ForwardDir * HitDir.dotProduct(ForwardDir)) * (1.0 - BallInteraction_PushForwardScale)
            HitDir = (HitDir - ForwardAdjustment).normaliseCopy()
            // EndIf

            val HitDirXY = HitDir.withZ(0.0)
            val pushFactor = ballPushFactorCurve(RelSpeed)
            val Impulse = (HitDir * RelSpeed) * pushFactor //  * (float(1) + Ball.ReplicatedAddedCarBounceScale)) + (HitDirXY * Ball.AdditionalCarGroundBounceScaleXY);
            // Impulse += (Impulse * AddedBallForceMultiplier)
            return Impulse
        }
        return Vector3.ZERO
    }

    fun calculatePhysicsBallImpactForce(carPosition: Vector3,
                                       carVelocity: Vector3,
                                       ballPosition: Vector3,
                                       ballVelocity: Vector3,
                                       carForwardDirectionNormal: Vector3): Vector3 {
        throw NotImplementedError()
    }
}
