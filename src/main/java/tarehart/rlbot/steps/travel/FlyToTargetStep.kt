package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.intercept.AerialMath
import tarehart.rlbot.math.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.MidairStrikeStep

class FlyToTargetStep(private val target: Vector3) : NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Flying to wall"
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        val toTarget = target - car.position

        val speedToTarget = VectorUtil.project(car.velocity, toTarget).magnitude()
        val timeToTarget = toTarget.magnitude() / speedToTarget

        if (timeToTarget < 1 || toTarget.magnitude() < 5) {
            return null
        }

        val courseResult = AerialMath.calculateAerialCourseCorrection(
                CarSlice(car),
                SpaceTime(target, bundle.agentInput.time.plusSeconds(1.0)),
                modelJump = false,
                secondsSinceJump = 0.0,
                assumeResidualBoostAccel = false)


        return OrientationSolver.orientCar(car, Mat3.lookingTo(courseResult.correctionDirection, car.orientation.roofVector), MidairStrikeStep.ORIENT_DT)
                .withBoost(true)
    }
}
