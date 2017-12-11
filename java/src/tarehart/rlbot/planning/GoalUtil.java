package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.Plane;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;

import java.util.Optional;

public class GoalUtil {

    public static final Goal BLUE_GOAL = new Goal(true);
    public static final Goal ORANGE_GOAL = new Goal(false);

    public static Goal getOwnGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? BLUE_GOAL : ORANGE_GOAL;
    }

    public static Goal getEnemyGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? ORANGE_GOAL : BLUE_GOAL;
    }

    public static Optional<BallSlice> predictGoalEvent(Goal goal, BallPath ballPath) {
        return ballPath.getPlaneBreak(ballPath.getStartPoint().time, goal.getScorePlane(), true);
    }

    public static boolean ballLingersInBox(Goal goal, BallPath ballPath) {
        Optional<BallSlice> firstSlice = ballPath.findSlice(slice -> goal.isInBox(slice.getSpace()));
        Optional<BallSlice> secondSlice = firstSlice.flatMap(stv -> ballPath.getMotionAt(stv.getTime().plusSeconds(2)));
        return secondSlice.isPresent() && goal.isInBox(secondSlice.get().getSpace());
    }

    public static Goal getNearestGoal(Vector3 position) {
        return position.y > 0 ? ORANGE_GOAL : BLUE_GOAL;
    }
}
