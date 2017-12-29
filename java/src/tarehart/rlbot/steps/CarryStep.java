package tarehart.rlbot.steps;

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
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.util.Optional;

import static tarehart.rlbot.planning.GoalUtil.getEnemyGoal;
import static tarehart.rlbot.tuning.BotLog.println;

public class CarryStep implements Step {
    private static final double MAX_X_DIFF = 1.3;
    private static final double MAX_Y = 1.5;
    private static final double MIN_Y = -0.9;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (!canCarry(input, true)) {
            return Optional.empty();
        }

        Vector2 ballVelocityFlat = input.getBallVelocity().flatten();
        double leadSeconds = .2;

        BallPath ballPath = ArenaModel.predictBallPath(input);

        Optional<BallSlice> motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1);
        if (motionAfterWallBounce.isPresent() && Duration.between(input.getTime(), motionAfterWallBounce.get().getTime()).toMillis() < 1000) {
            return Optional.empty(); // The dribble step is not in the business of wall reads.
        }

        Vector2 futureBallPosition;
        BallSlice ballFuture = ballPath.getMotionAt(input.getTime().plusSeconds(leadSeconds)).get();
        futureBallPosition = ballFuture.getSpace().flatten();


        Vector2 scoreLocation = getEnemyGoal(input.getTeam()).getNearestEntrance(input.getBallPosition(), 3).flatten();

        Vector2 ballToGoal = scoreLocation.minus(futureBallPosition);
        Vector2 pushDirection;
        Vector2 pressurePoint;
        double approachDistance = 1;
        // TODO: vary the approachDistance based on whether the ball is forward / off to the side.

        double velocityCorrectionAngle = ballVelocityFlat.correctionAngle(ballToGoal);
        double angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2));
        pushDirection = VectorUtil.rotateVector(ballToGoal, angleTweak).normalized();
        pressurePoint = futureBallPosition.minus(pushDirection.scaled(approachDistance));


        GameTime hurryUp = input.getTime().plusSeconds(leadSeconds);

        AgentOutput dribble = SteerUtil.getThereOnTime(input.getMyCarData(), new SpaceTime(new Vector3(pressurePoint.getX(), pressurePoint.getY(), 0), hurryUp));
        return Optional.of(dribble);
    }

    private static Vector3 positionInCarCoordinates(CarData car, Vector3 worldPosition) {
        // We will assume that the car is flat on the ground.

        // We will treat (0, 1) as the car's natural orientation.
        double carYaw = new Vector2(0, 1).correctionAngle(car.getOrientation().getNoseVector().flatten());

        Vector2 carToPosition = worldPosition.minus(car.getPosition()).flatten();

        Vector2 carToPositionRotated = VectorUtil.rotateVector(carToPosition, -carYaw);

        double zDiff = worldPosition.getZ() - car.getPosition().getZ();
        return new Vector3(carToPositionRotated.getX(), carToPositionRotated.getY(), zDiff);
    }

    public static boolean canCarry(AgentInput input, boolean log) {

        CarData car = input.getMyCarData();
        Vector3 ballInCarCoordinates = positionInCarCoordinates(car, input.getBallPosition());

        double xMag = Math.abs(ballInCarCoordinates.getX());
        if (xMag > MAX_X_DIFF) {
            if (log) {
                println("Fell off the side", input.getPlayerIndex());
            }
            return false;
        }

        if (ballInCarCoordinates.getY() > MAX_Y) {
            if (log) {
                println("Fell off the front", input.getPlayerIndex());
            }
            return false;
        }

        if (ballInCarCoordinates.getY() < MIN_Y) {
            if (log) {
                println("Fell off the back", input.getPlayerIndex());
            }
            return false;
        }

        if (ballInCarCoordinates.getZ() > 3) {
            if (log) {
                println("Ball too high to carry", input.getPlayerIndex());
            }
            return false;
        }

        if (ballInCarCoordinates.getZ() < 1) {
            if (log) {
                println("Ball too low to carry", input.getPlayerIndex());
            }
            return false;
        }

        if (VectorUtil.flatDistance(car.getVelocity(), input.getBallVelocity()) > 10) {
            if (log) {
                println("Velocity too different to carry.", input.getPlayerIndex());
            }
            return false;
        }


        return true;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public String getSituation() {
        return "Carrying";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
