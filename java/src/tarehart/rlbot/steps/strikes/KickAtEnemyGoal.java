package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.TacticsAdvisor;

public class KickAtEnemyGoal implements KickStrategy {
    @Override
    public Vector3 getKickDirection(AgentInput input) {
        return getKickDirection(input, input.getBallPosition());
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition) {
        CarData car = input.getMyCarData();
        Vector3 toBall = ballPosition.minus(car.getPosition());
        return getDirection(car, ballPosition, toBall);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition, Vector3 easyKick) {
        return getDirection(input.getMyCarData(), ballPosition, easyKick);
    }

    @Override
    public boolean looksViable(CarData car, Vector3 ballPosition) {
        return TacticsAdvisor.Companion.generousShotAngle(GoalUtil.INSTANCE.getEnemyGoal(car.getTeam()), ballPosition.flatten(), car.getPlayerIndex());
    }

    private Vector3 getDirection(CarData car, Vector3 ballPosition, Vector3 easyKick) {
        Vector2 easyKickFlat = easyKick.flatten();
        Vector2 toLeftCorner = GoalUtil.INSTANCE.getEnemyGoal(car.getTeam()).getLeftPost(6).minus(ballPosition).flatten();
        Vector2 toRightCorner = GoalUtil.INSTANCE.getEnemyGoal(car.getTeam()).getRightPost(6).minus(ballPosition).flatten();

        double rightCornerCorrection = easyKickFlat.correctionAngle(toRightCorner);
        double leftCornerCorrection = easyKickFlat.correctionAngle(toLeftCorner);
        if (rightCornerCorrection < 0 && leftCornerCorrection > 0) {
            // The easy kick is already on target. Go with the easy kick.
            return new Vector3(easyKickFlat.getX(), easyKickFlat.getY(), 0);
        } else if (Math.abs(rightCornerCorrection) < Math.abs(leftCornerCorrection)) {
            return new Vector3(toRightCorner.getX(), toRightCorner.getY(), 0);
        } else {
            return new Vector3(toLeftCorner.getX(), toLeftCorner.getY(), 0);
        }
    }
}
