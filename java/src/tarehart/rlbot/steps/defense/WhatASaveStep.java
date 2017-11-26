package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.strikes.DirectedSideHitStep;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal;

import java.awt.*;
import java.time.Duration;
import java.util.Optional;

public class WhatASaveStep implements Step {
    private Plan plan;
    private Double whichPost;

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        CarData car = input.getMyCarData();
        BallPath ballPath = ArenaModel.predictBallPath(input);
        Goal goal = GoalUtil.getOwnGoal(input.team);
        Optional<BallSlice> currentThreat = GoalUtil.predictGoalEvent(goal, ballPath);
        if (!currentThreat.isPresent()) {
            return Optional.empty();
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


        SpaceTime intercept = SteerUtil.getInterceptOpportunity(car, ballPath, plot).orElse(threat.toSpaceTime());

        Vector3 carToIntercept = intercept.space.minus(car.position);
        double carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(input.ballVelocity.flatten());
        if (Math.abs(carApproachVsBallApproach) > Math.PI / 5) {

            if (Vector2.angle(car.orientation.noseVector.flatten(), carToIntercept.flatten()) < Math.PI / 12) {

                plan = new Plan(Plan.Posture.SAVE).withStep(new InterceptStep(new Vector3(0, Math.signum(goal.getCenter().y) * 1.5, 0)));
                return plan.getOutput(input);
            } else {
                return Optional.of(SteerUtil.steerTowardGroundPosition(car, intercept.space));
            }
        }

        plan = new Plan().withStep(new DirectedSideHitStep(new KickAwayFromOwnGoal()));
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
        // Draw nothing.
    }
}
