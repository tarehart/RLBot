package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.planning.GoalUtil;

public class FunnelTowardEnemyGoal implements KickStrategy {
    @Override
    public Vector3 getKickDirection(AgentInput input) {
        return getKickDirection(input, input.ballPosition);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition) {
        CarData car = input.getMyCarData();
        Vector3 toBall = ballPosition.minus(car.position);
        return getDirection(car, ballPosition, toBall);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition, Vector3 easyKick) {
        return getDirection(input.getMyCarData(), ballPosition, easyKick);
    }

    private Vector3 getDirection(CarData car, Vector3 ballPosition, Vector3 easyKick) {
        Vector2 easyKickFlat = easyKick.flatten();
        Vector2 idealKick = getIdealDirection(car, ballPosition);

        if (Vector2.angle(easyKickFlat, idealKick) < Math.PI / 8) {
            return easyKick;
        }

        return new Vector3(idealKick.x, idealKick.y, 0);
    }

    private Vector2 getIdealDirection(CarData car, Vector3 ballPosition) {
        Goal enemyGoal = GoalUtil.getEnemyGoal(car.team);
        if (enemyGoal.getCenter().y * ballPosition.y < 0) {
            // Ball is not on the enemy side. Strange that you're using this strat.
            return new Vector2(0, Math.signum(enemyGoal.getCenter().y));
        }

        if (Math.abs(ballPosition.x) > 60) {
            return new Vector2(0, Math.signum(ballPosition.y)); // bounce off corner toward goal
        }

        Vector3 toEnemyGoal = enemyGoal.getCenter().minus(ballPosition);
        Vector3 angleUpWall = new Vector3(Math.signum(toEnemyGoal.x), Math.signum(enemyGoal.getCenter().y), 0);
        return angleUpWall.flatten().normalized();
    }
}
