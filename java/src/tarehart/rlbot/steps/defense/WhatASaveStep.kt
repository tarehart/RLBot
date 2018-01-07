package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AirTouchPlanner
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.Step
import tarehart.rlbot.steps.strikes.DirectedSideHitStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.time.Duration

import java.awt.*
import java.util.Optional

class WhatASaveStep : NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Making a save"
    }

    private var whichPost: Double? = null
    private var goingForSuperJump: Boolean = false

    override fun getUnplannedOutput(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData
        val ballPath = ArenaModel.predictBallPath(input)
        val goal = GoalUtil.getOwnGoal(input.team)
        val currentThreat = GoalUtil.predictGoalEvent(goal, ballPath) ?: return Optional.empty()

        if (whichPost == null) {

            val carToThreat = currentThreat.space - car.position
            val carApproachVsBallApproach = carToThreat.flatten().correctionAngle(input.ballVelocity.flatten())
            // When carApproachVsBallApproach < 0, car is to the right of the ball, angle wise. Right is positive X when we're on the positive Y side of the field.
            whichPost = Math.signum(-carApproachVsBallApproach * currentThreat.space.y)

        }

        val distance = VectorUtil.flatDistance(car.position, currentThreat.space)
        val plot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5.0), car.boost, distance - 15)


        val (space1) = InterceptCalculator.getInterceptOpportunity(car, ballPath, plot)
                .orElse(Intercept(currentThreat.space, currentThreat.time, 0.0, StrikeProfile(), plot, Duration.ofMillis(0)))

        val carToIntercept = space1.minus(car.position)
        val carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(input.ballVelocity.flatten())

        val overHeadSlice = ballPath.findSlice { (space2) -> car.position.flatten().distance(space2.flatten()) < ArenaModel.BALL_RADIUS }

        if (overHeadSlice.isPresent && (goingForSuperJump || AirTouchPlanner.isVerticallyAccessible(car, overHeadSlice.get().toSpaceTime()))) {

            goingForSuperJump = true

            val overheadHeight = overHeadSlice.get().space.z
            if (AirTouchPlanner.expectedSecondsForSuperJump(overheadHeight) >= Duration.between(input.time, overHeadSlice.get().time).seconds) {
                return startPlan(SetPieces.jumpSuperHigh(overheadHeight), input)
            } else {
                return Optional.of(AgentOutput())
            }
        }

        if (Math.abs(carApproachVsBallApproach) > Math.PI / 5) {
            return startPlan(
                    Plan(Plan.Posture.SAVE).withStep(InterceptStep(Vector3(0.0, Math.signum(goal.center.y) * 1.5, 0.0))),
                    input)
        }

        return startPlan(FirstViableStepPlan(Plan.Posture.SAVE)
                .withStep(DirectedSideHitStep(KickAwayFromOwnGoal()))
                .withStep(InterceptStep(Vector3(0.0, 0.0, -1.0))),
                input)
    }
}
