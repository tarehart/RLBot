package tarehart.rlbot.physics

import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.tuning.ManeuverMath
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min

object BallPhysics {

    val BALL_MASS = 30.0F
    val CAR_MASS = 180.0F

    val UU_CONST = Vector3.PACKET_DISTANCE_TO_CLASSIC.toFloat()
    val pushFactorCurve = mapOf(
            0    / UU_CONST to 0.65F,
            500  / UU_CONST to 0.6F,
            1400 / UU_CONST to 0.55F,
            2300 / UU_CONST to 0.5F,
            4600 / UU_CONST to 0.3F)

    fun getGroundBounceEnergy(height: Float, verticalVelocity: Float): Double {
        val potentialEnergy = (height - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY
        val verticalKineticEnergy = 0.5 * verticalVelocity * verticalVelocity
        return potentialEnergy + verticalKineticEnergy
    }

    private fun ballPushFactorCurve(relativeSpeed: Float): Float {
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
        return 0.65F
    }

    // https://gist.github.com/nevercast/407cc224d5017622dbbd92e70f7c9823
    // TODO: It may be worth tidying this up, I've kept it close to source for validation sake
    fun calculateScriptBallImpactForce(carPosition: Vector3,
                                       carVelocity: Vector3,
                                       ballPosition: Vector3,
                                       ballVelocity: Vector3,
                                       carForwardDirectionNormal: Vector3): Vector3 {
        val BallInteraction_MaxRelativeSpeed = 4600.0F
        val BallInteraction_PushZScale = 0.35F
        val BallInteraction_PushForwardScale = 0.65F

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

            val pushFactor = ballPushFactorCurve(RelSpeed)
            val Impulse = (HitDir * RelSpeed) * pushFactor //  * (float(1) + Ball.ReplicatedAddedCarBounceScale)) + (HitDirXY * Ball.AdditionalCarGroundBounceScaleXY);
            // Impulse += (Impulse * AddedBallForceMultiplier)
            return Impulse
        }
        return Vector3.ZERO
    }

    // This is an elastic collision, we could also calculate the car change in velocity
    // Note: If you added a car version, it would not work for suspension damping
    // TODO: This needs slip velocity (rotation)
    // Note: Rocket League's collision model is perfectly inelastic, but then the script impulse above is applied
    fun calculateCarBallCollisionImpulse( carPosition: Vector3, carVelocity: Vector3, ballPosition: Vector3, ballVelocity: Vector3): Vector3 {
        // J = (m1 * m2 / (m1 + m2)) * (v2 - v1)
        // val resistution = 0.0
        val normal = (carPosition - ballPosition).normaliseCopy()
        val carVelCollision = carVelocity.dotProduct(normal)
        val ballVelCollision = ballVelocity.dotProduct(normal)
        val impulse = (carVelCollision - ballVelCollision) * (CAR_MASS * BALL_MASS / (CAR_MASS + BALL_MASS))
        return normal * impulse
    }

    fun predictBallVelocity(carSlice: CarSlice, ballPosition: Vector3, ballVelocity: Vector3): Vector3 {
        val inelasticImpulse = BallPhysics.calculateCarBallCollisionImpulse(
                carPosition = carSlice.space,
                carVelocity = carSlice.velocity,
                ballPosition = ballPosition,
                ballVelocity = ballVelocity)

        val scriptImpulse = BallPhysics.calculateScriptBallImpactForce(
                carPosition = carSlice.space,
                carVelocity = carSlice.velocity,
                ballPosition = ballPosition,
                ballVelocity = ballVelocity,
                carForwardDirectionNormal = carSlice.orientation.noseVector)

        return inelasticImpulse / 30.0 + scriptImpulse
    }

    fun computeChipOptions(currentCarPosition: Vector3, arrivalSpeed: Double, ballSlice: BallSlice, hitbox: CarHitbox): List<Pair<Vector3, Vector3>> {
        val hitboxSideExtent = hitbox.sidewaysExtent
        val hitboxForwardExtent = hitbox.forwardExtent
        val contactHeight = ManeuverMath.BASE_CAR_Z + hitbox.upwardExtent
        val chipRingRadius = cos(asin((ballSlice.space.z - contactHeight) / ArenaModel.BALL_RADIUS)) * ArenaModel.BALL_RADIUS
        val toSlice = (ballSlice.space - currentCarPosition).withZ(0)
        val toSliceNormal = toSlice.normaliseCopy()
        val orthogonal = toSliceNormal.crossProduct(Vector3.UP)

        val options = ArrayList<Pair<Vector3, Vector3>>()

        for (i in 0..25) {
            val offsetAmount = i * 0.1F

            // TODO: use this aim point so we can account for the different approach angle when aiming at an offset.
            val aimPoint = ballSlice.space + orthogonal * offsetAmount
            val offsetBackoffMagnitude: Float
            if (offsetAmount > hitboxSideExtent) {
                offsetBackoffMagnitude = cos( asin(offsetAmount - hitboxSideExtent)) * chipRingRadius
            } else {
                offsetBackoffMagnitude = chipRingRadius
            }
            val offset = toSliceNormal * (-offsetBackoffMagnitude - hitboxForwardExtent) +
                    orthogonal * offsetAmount +
                    Vector3.UP * (ManeuverMath.BASE_CAR_Z - ballSlice.space.z)

            val carSlice = CarSlice(
                    space = ballSlice.space + offset,
                    time = ballSlice.time,
                    velocity = toSliceNormal * arrivalSpeed,
                    orientation = CarOrientation(toSliceNormal, Vector3.UP))

            val predictedVelocity = predictBallVelocity(carSlice, ballSlice.space, ballSlice.velocity)

            options.add(Pair(offset, predictedVelocity))
        }

        return options
    }
}
