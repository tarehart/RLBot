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
        return true;
    }

    private Vector3 getDirection(CarData car, Vector3 ballPosition, Vector3 easyKick) {
        Vector2 easyKickFlat = easyKick.flatten();
        Vector2 idealKick = getIdealDirection(car, ballPosition);

        if (Vector2.Companion.angle(easyKickFlat, idealKick) < Math.PI / 8) {
            return easyKick;
        }

        return new Vector3(idealKick.getX(), idealKick.getY(), 0);
    }

    private Vector2 getIdealDirection(CarData car, Vector3 ballPosition) {
        Goal enemyGoal = GoalUtil.INSTANCE.getEnemyGoal(car.getTeam());
        if (enemyGoal.getCenter().getY() * ballPosition.getY() < 0) {
            // Ball is not on the enemy side. Strange that you're using this strat.
            return new Vector2(0, Math.signum(enemyGoal.getCenter().getY()));
        }

        if (Math.abs(ballPosition.getX()) > 60) {
            return new Vector2(0, Math.signum(ballPosition.getY())); // bounce off corner toward goal
        }

        Vector3 toEnemyGoal = enemyGoal.getCenter().minus(ballPosition);
        Vector3 angleUpWall = new Vector3(Math.signum(toEnemyGoal.getX()), Math.signum(enemyGoal.getCenter().getY()), 0);
        return angleUpWall.flatten().normalized();
    }
}
