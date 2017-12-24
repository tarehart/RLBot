package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.strikes.DirectedSideHitStep;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal;
import tarehart.rlbot.time.Duration;

import java.awt.*;
import java.util.Optional;

public class WhatASaveStep implements Step {
    private Plan plan;
    private Double whichPost;
    private boolean goingForSuperJump;

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        BallPath ballPath = ArenaModel.predictBallPath(input);
        Goal goal = GoalUtil.getOwnGoal(input.team);
        Optional<BallSlice> currentThreat = GoalUtil.predictGoalEvent(goal, ballPath);
        if (!currentThreat.isPresent()) {
            return Optional.empty();
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        BallSlice threat = currentThreat.get();

        if (whichPost == null) {

            Vector3 carToThreat = threat.space.minus(car.position);
            double carApproachVsBallApproach = carToThreat.flatten().correctionAngle(input.ballVelocity.flatten());
            // When carApproachVsBallApproach < 0, car is to the right of the ball, angle wise. Right is positive X when we're on the positive Y side of the field.
            whichPost = Math.signum(-carApproachVsBallApproach * threat.space.y);

        }

        double distance = VectorUtil.flatDistance(car.position, threat.getSpace());
        DistancePlot plot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5), car.boost, distance - 15);


        Intercept intercept = SteerUtil.getInterceptOpportunity(car, ballPath, plot)
                .orElse(new Intercept(threat.toSpaceTime(), new StrikeProfile(0, 0, 0), plot));

        Vector3 carToIntercept = intercept.getSpace().minus(car.position);
        double carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(input.ballVelocity.flatten());

        Optional<BallSlice> overHeadSlice = ballPath.findSlice((ballSlice -> {
            return car.position.flatten().distance(ballSlice.space.flatten()) < ArenaModel.BALL_RADIUS;
        }));

        if (overHeadSlice.isPresent() && (goingForSuperJump || AirTouchPlanner.isVerticallyAccessible(car, overHeadSlice.get().toSpaceTime()))) {

            goingForSuperJump = true;

            double overheadHeight = overHeadSlice.get().space.z;
            if (AirTouchPlanner.expectedSecondsForSuperJump(overheadHeight) >= Duration.between(input.time, overHeadSlice.get().time).getSeconds()) {
                plan = SetPieces.jumpSuperHigh(overheadHeight);
                return plan.getOutput(input);
            } else {
                return Optional.of(new AgentOutput());
            }
        }

        if (Math.abs(carApproachVsBallApproach) > Math.PI / 5) {
            plan = new Plan(Plan.Posture.SAVE).withStep(new InterceptStep(new Vector3(0, Math.signum(goal.getCenter().y) * 1.5, 0)));
            return plan.getOutput(input);
        }

        plan = new FirstViableStepPlan(Plan.Posture.SAVE).withStep(new DirectedSideHitStep(new KickAwayFromOwnGoal())).withStep(new InterceptStep(new Vector3(0, 0, -1)));
        return plan.getOutput(input);
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Making a save", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        Plan.activePlan(plan).ifPresent(p -> p.getCurrentStep().drawDebugInfo(graphics));
    }
}
