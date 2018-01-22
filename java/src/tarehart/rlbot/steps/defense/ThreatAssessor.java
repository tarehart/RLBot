package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.InterceptCalculator;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.time.Duration;

import java.util.Optional;

public class ThreatAssessor {


    public double measureThreat(AgentInput input, Optional<CarData> enemyCarOption) {

        double enemyPosture = measureEnemyPosture(input, enemyCarOption);
        double enemyInitiative = measureEnemyInitiative(input, enemyCarOption);
        double ballThreat = measureBallThreat(input) *  .3;

        double enemyThreat = enemyPosture > 0 && enemyInitiative > .2 ? 10 : 0;

        return enemyThreat + ballThreat;

    }

    private double measureEnemyInitiative(AgentInput input, Optional<CarData> enemyCarOption) {

        if (!enemyCarOption.isPresent()) {
            return 0;
        }
        CarData enemyCar = enemyCarOption.get();

        Duration simDuration = Duration.Companion.ofSeconds(4);
        BallPath ballPath = ArenaModel.predictBallPath(input);

        CarData myCar = input.getMyCarData();

        Optional<Intercept> myInterceptOption = InterceptCalculator.INSTANCE.getInterceptOpportunityAssumingMaxAccel(myCar, ballPath, myCar.getBoost());
        Optional<Intercept> enemyInterceptOption = InterceptCalculator.INSTANCE.getInterceptOpportunityAssumingMaxAccel(enemyCar, ballPath, enemyCar.getBoost());

        if (!enemyInterceptOption.isPresent()) {
            return 0;
        }

        if (!myInterceptOption.isPresent()) {
            return 3;
        }

        Intercept myIntercept = myInterceptOption.get();
        Intercept enemyIntercept = enemyInterceptOption.get();

        return Duration.Companion.between(myIntercept.getTime(), enemyIntercept.getTime()).getSeconds();
    }

    private double measureEnemyPosture(AgentInput input, Optional<CarData> enemyCarOption) {

        if (!enemyCarOption.isPresent()) {
            return 0;
        }
        CarData enemyCar = enemyCarOption.get();
        
        Goal myGoal = GoalUtil.INSTANCE.getOwnGoal(input.getTeam());
        Vector3 ballToGoal = myGoal.getCenter().minus(input.getBallPosition());

        Vector3 carToBall = input.getBallPosition().minus(enemyCar.getPosition());
        Vector3 rightSideVector = VectorUtil.INSTANCE.project(carToBall, ballToGoal);

        return rightSideVector.magnitude() * Math.signum(rightSideVector.dotProduct(ballToGoal));
    }


    private double measureBallThreat(AgentInput input) {

        CarData car = input.getMyCarData();
        Goal myGoal = GoalUtil.INSTANCE.getOwnGoal(input.getTeam());
        Vector3 ballToGoal = myGoal.getCenter().minus(input.getBallPosition());

        Vector3 ballVelocityTowardGoal = VectorUtil.INSTANCE.project(input.getBallVelocity(), ballToGoal);
        double ballSpeedTowardGoal = ballVelocityTowardGoal.magnitude() * Math.signum(ballVelocityTowardGoal.dotProduct(ballToGoal));

        Vector3 carToBall = input.getBallPosition().minus(car.getPosition());
        Vector3 wrongSideVector = VectorUtil.INSTANCE.project(carToBall, ballToGoal);
        double wrongSidedness = wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal));

        return ballSpeedTowardGoal + wrongSidedness;
    }

}
