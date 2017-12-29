package tarehart.rlbot.steps.wall;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.InterceptCalculator;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.time.Duration;

import java.awt.*;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static tarehart.rlbot.tuning.BotLog.println;

public class WallTouchStep implements Step {
    public static final double ACCEPTABLE_WALL_DISTANCE = ArenaModel.BALL_RADIUS + 5;
    public static final double WALL_DEPART_SPEED = 10;
    private static final double MIN_HEIGHT = 10;
    private Vector3 originalIntercept;

    private static boolean isBallOnWall(CarData car, SpaceTime ballPosition) {
        return ballPosition.space.z > MIN_HEIGHT && ArenaModel.getDistanceFromWall(ballPosition.space) <= ACCEPTABLE_WALL_DISTANCE;
    }

    private static boolean isBallOnWall(BallSlice ballPosition) {
        return ballPosition.space.z > MIN_HEIGHT && ArenaModel.getDistanceFromWall(ballPosition.space) <= ACCEPTABLE_WALL_DISTANCE;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (!ArenaModel.isCarOnWall(car)) {
            println("Failed to make the wall touch because the car is not on the wall", input.playerIndex);
            return empty();
        }


        BallPath ballPath = ArenaModel.predictBallPath(input);
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Optional<Intercept> interceptOpportunity = InterceptCalculator.getFilteredInterceptOpportunity(car, ballPath, fullAcceleration, new Vector3(), WallTouchStep::isBallOnWall);
        Optional<BallSlice> ballMotion = interceptOpportunity.flatMap(inter -> ballPath.getMotionAt(inter.getTime()));


        if (!ballMotion.isPresent()) {
            println("Failed to make the wall touch because we see no intercepts on the wall", input.playerIndex);
            return empty();
        }
        BallSlice motion = ballMotion.get();


        if (originalIntercept == null) {
            originalIntercept = motion.getSpace();
        } else {
            if (originalIntercept.distance(motion.getSpace()) > 20) {
                println("Failed to make the wall touch because the intercept changed", input.playerIndex);
                return empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        if (readyToJump(input, motion.toSpaceTime())) {
            println("Jumping for wall touch.", input.playerIndex);
            return of(new AgentOutput().withAcceleration(1).withJump());
        }

        return Optional.of(SteerUtil.steerTowardWallPosition(car, motion.space));
    }

    private static boolean readyToJump(AgentInput input, SpaceTime carPositionAtContact) {

        if (ArenaModel.getDistanceFromWall(carPositionAtContact.space) > ArenaModel.BALL_RADIUS + .5) {
            return false; // Really close to wall, no need to jump. Just chip it.
        }
        CarData car = input.getMyCarData();
        Vector3 toPosition = carPositionAtContact.space.minus(car.position);
        double correctionAngleRad = VectorUtil.getCorrectionAngle(car.orientation.noseVector, toPosition, car.orientation.roofVector);
        double secondsTillIntercept = Duration.between(input.time, carPositionAtContact.time).getSeconds();
        double wallDistanceAtIntercept = ArenaModel.getDistanceFromWall(carPositionAtContact.space);
        double tMinus = secondsTillIntercept - wallDistanceAtIntercept / WALL_DEPART_SPEED;
        boolean linedUp = Math.abs(correctionAngleRad) < Math.PI / 8;
        if (tMinus < 3) {
            println("Correction angle: " + correctionAngleRad, input.playerIndex);
        }

        return tMinus < 0.1 && tMinus > -.4 && linedUp;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public String getSituation() {
        return "Making a wall touch.";
    }

    public static boolean hasWallTouchOpportunity(AgentInput input, BallPath ballPath) {

        Optional<BallSlice> nearWallOption = ballPath.findSlice(WallTouchStep::isBallOnWall);
        if (nearWallOption.isPresent()) {
            BallSlice nearWall = nearWallOption.get();
            if (Duration.between(input.time, nearWall.getTime()).getSeconds() > 3) {
                return false; // Not on wall soon enough
            }

            Optional<BallSlice> ballLater = ballPath.getMotionAt(nearWall.getTime().plusSeconds(1));
            if (ballLater.isPresent()) {
                BallSlice motion = ballLater.get();
                if (ArenaModel.getDistanceFromWall(motion.getSpace()) > ACCEPTABLE_WALL_DISTANCE) {
                    return false;
                }
                Vector3 ownGoalCenter = GoalUtil.getOwnGoal(input.team).getCenter();
                return (motion.getSpace().distance(ownGoalCenter) > input.getMyCarData().position.distance(ownGoalCenter));
            }

        }

        return false;
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
