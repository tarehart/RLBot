package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.util.Optional;

public class GoForKickoffStep implements Step {

    private static final double DIAGONAL_KICKOFF_X = 40.98;
    private static final double CHEATER_KICKOFF_X = 5.09;
    private static final double CENTER_KICKOFF_X = 0;
    private static final double WIGGLE_ROOM = 2;
    private static final double CHEATIN_BOOST_Y = 58;

    private Plan plan;
    private KickoffType kickoffType;

    private enum KickoffType {
        CENTER,
        CHEATIN,
        SLANTERD,
        UNKNOWN
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (input.ballPosition.flatten().magnitudeSquared() > 0) {
            return Optional.empty();
        }

        CarData car = input.getMyCarData();

        if (kickoffType == null) {
            kickoffType = getKickoffType(car);
        }

        double distance = car.position.magnitude();
        if (distance < 14) {
            plan = SetPieces.frontFlip();
            return plan.getOutput(input);
        }

        double ySide = Math.signum(car.position.y);

        Vector2 target;
        if (kickoffType == KickoffType.CHEATIN && Math.abs(car.position.y) > CHEATIN_BOOST_Y + 10) {
            // Steer toward boost
            target = new Vector2(0, ySide * CHEATIN_BOOST_Y);
        } else if (distance > 30) {
            target = new Vector2(0, ySide * 15);
        } else {
            target = new Vector2(0, 0);
        }
        return Optional.of(SteerUtil.steerTowardGroundPosition(car, target));
    }

    private KickoffType getKickoffType(CarData car) {
        double xPosition = car.position.x;
        if (getNumberDistance(CENTER_KICKOFF_X, xPosition) < WIGGLE_ROOM){
            BotLog.println("it be center" , car.team);
            return KickoffType.CENTER;
        }

        if (getNumberDistance(CHEATER_KICKOFF_X, Math.abs(xPosition)) < WIGGLE_ROOM){
            BotLog.println("it be cheatin" , car.team);
            return KickoffType.CHEATIN;
        }

        if (getNumberDistance(DIAGONAL_KICKOFF_X , Math.abs(xPosition)) < WIGGLE_ROOM){
            BotLog.println("it be slanterd" , car.team);
            return KickoffType.SLANTERD;
        }

        BotLog.println("what on earth" , car.team);
        return KickoffType.UNKNOWN;
    }

    private static double getNumberDistance(double first, double second){
        return Math.abs(first - second);
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Going for kickoff", plan);
    }
}
