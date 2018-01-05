package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.AirTouchPlanner;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.intercept.InterceptCalculator;
import tarehart.rlbot.intercept.StrikeProfile;
import tarehart.rlbot.math.BallSlice;
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
        Goal goal = GoalUtil.getOwnGoal(input.getTeam());
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

            Vector3 carToThreat = threat.getSpace().minus(car.getPosition());
            double carApproachVsBallApproach = carToThreat.flatten().correctionAngle(input.getBallVelocity().flatten());
            // When carApproachVsBallApproach < 0, car is to the right of the ball, angle wise. Right is positive X when we're on the positive Y side of the field.
            whichPost = Math.signum(-carApproachVsBallApproach * threat.getSpace().getY());

        }

        double distance = VectorUtil.INSTANCE.flatDistance(car.getPosition(), threat.getSpace());
        DistancePlot plot = AccelerationModel.INSTANCE.simulateAcceleration(car, Duration.Companion.ofSeconds(5), car.getBoost(), distance - 15);


        Intercept intercept = InterceptCalculator.INSTANCE.getInterceptOpportunity(car, ballPath, plot)
                .orElse(new Intercept(threat.toSpaceTime(), new StrikeProfile(), plot));

        Vector3 carToIntercept = intercept.getSpace().minus(car.getPosition());
        double carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(input.getBallVelocity().flatten());

        Optional<BallSlice> overHeadSlice = ballPath.findSlice((ballSlice -> {
            return car.getPosition().flatten().distance(ballSlice.getSpace().flatten()) < ArenaModel.BALL_RADIUS;
        }));

        if (overHeadSlice.isPresent() && (goingForSuperJump || AirTouchPlanner.isVerticallyAccessible(car, overHeadSlice.get().toSpaceTime()))) {

            goingForSuperJump = true;

            double overheadHeight = overHeadSlice.get().getSpace().getZ();
            if (AirTouchPlanner.expectedSecondsForSuperJump(overheadHeight) >= Duration.Companion.between(input.getTime(), overHeadSlice.get().getTime()).getSeconds()) {
                plan = SetPieces.jumpSuperHigh(overheadHeight);
                return plan.getOutput(input);
            } else {
                return Optional.of(new AgentOutput());
            }
        }

        if (Math.abs(carApproachVsBallApproach) > Math.PI / 5) {
            plan = new Plan(Plan.Posture.SAVE).withStep(new InterceptStep(new Vector3(0, Math.signum(goal.getCenter().getY()) * 1.5, 0)));
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
        return Plan.Companion.concatSituation("Making a save", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        Plan.Companion.activePlan(plan).ifPresent(p -> p.getCurrentStep().drawDebugInfo(graphics));
    }
}
