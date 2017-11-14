package tarehart.rlbot.steps.defense;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;
import java.util.Optional;

public class ThreatAssessor {


    public double measureThreat(AgentInput input) {

        double enemyPosture = measureEnemyPosture(input);
        double enemyInitiative = measureEnemyInitiative(input);
        double ballThreat = measureBallThreat(input) *  .3;

        double enemyThreat = enemyPosture > 0 && enemyInitiative > .2 ? 10 : 0;

        return enemyThreat + ballThreat;

    }

    private double measureEnemyInitiative(AgentInput input) {

        Optional<CarData> enemyCarOption = input.getEnemyCarData();
        if (!enemyCarOption.isPresent()) {
            return 0;
        }
        CarData enemyCar = enemyCarOption.get();

        Duration simDuration = Duration.ofSeconds(4);
        BallPath ballPath = ArenaModel.predictBallPath(input);

        CarData myCar = input.getMyCarData();

        Optional<SpaceTime> myInterceptOption = SteerUtil.getInterceptOpportunityAssumingMaxAccel(myCar, ballPath, myCar.boost);
        Optional<SpaceTime> enemyInterceptOption = SteerUtil.getInterceptOpportunityAssumingMaxAccel(enemyCar, ballPath, enemyCar.boost);

        if (!enemyInterceptOption.isPresent()) {
            return 0;
        }

        if (!myInterceptOption.isPresent()) {
            return 3;
        }

        SpaceTime myIntercept = myInterceptOption.get();
        SpaceTime enemyIntercept = enemyInterceptOption.get();

        return TimeUtil.secondsBetween(myIntercept.time, enemyIntercept.time);
    }

    private double measureEnemyPosture(AgentInput input) {

        Optional<CarData> enemyCarOption = input.getEnemyCarData();
        if (!enemyCarOption.isPresent()) {
            return 0;
        }
        CarData enemyCar = enemyCarOption.get();
        
        Goal myGoal = GoalUtil.getOwnGoal(input.team);
        Vector3 ballToGoal = myGoal.getCenter().minus(input.ballPosition);

        Vector3 carToBall = input.ballPosition.minus(enemyCar.position);
        Vector3 rightSideVector = VectorUtil.project(carToBall, ballToGoal);

        return rightSideVector.magnitude() * Math.signum(rightSideVector.dotProduct(ballToGoal));
    }


    private double measureBallThreat(AgentInput input) {

        CarData car = input.getMyCarData();
        Goal myGoal = GoalUtil.getOwnGoal(input.team);
        Vector3 ballToGoal = myGoal.getCenter().minus(input.ballPosition);

        Vector3 ballVelocityTowardGoal = VectorUtil.project(input.ballVelocity, ballToGoal);
        double ballSpeedTowardGoal = ballVelocityTowardGoal.magnitude() * Math.signum(ballVelocityTowardGoal.dotProduct(ballToGoal));

        Vector3 carToBall = input.ballPosition.minus(car.position);
        Vector3 wrongSideVector = VectorUtil.project(carToBall, ballToGoal);
        double wrongSidedness = wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal));

        return ballSpeedTowardGoal + wrongSidedness;
    }

}
