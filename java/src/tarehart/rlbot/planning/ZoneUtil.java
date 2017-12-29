package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;

public class ZoneUtil {

    // Checks to see if the y position order is: my car, opponent car, ball, my net
    public static boolean isEnemyOffensiveBreakaway(Bot.Team team, CarData myCar, CarData opponentCar, Vector3 ballPosition) {
        if(team == Bot.Team.BLUE) {
            return myCar.position.getY() > opponentCar.position.getY() && opponentCar.position.getY() > ballPosition.getY();
        }
        else {
            return myCar.position.getY() < opponentCar.position.getY() && opponentCar.position.getY() < ballPosition.getY();
        }
    }

    // Checks to see if the y position order is: opponent car, my car, ball, their net
    public static boolean isMyOffensiveBreakaway(Bot.Team team, CarData myCar, CarData opponentCar, Vector3 ballPosition) {
        if(team == Bot.Team.BLUE) {
            return ballPosition.getY() > myCar.position.getY() && myCar.position.getY() > opponentCar.position.getY();
        }
        else {
            return ballPosition.getY() < myCar.position.getY() && myCar.position.getY() < opponentCar.position.getY();
        }
    }

    public static Polygon getShotDefenseZone(Vector3 ballPosition, Vector2 goalCenter) {
        return new Polygon(new Vector2[] {
                ballPosition.flatten(),
                new Vector2(goalCenter.getX() + Goal.EXTENT, goalCenter.getY()),
                new Vector2(goalCenter.getX() - Goal.EXTENT, goalCenter.getY())
        });
    }

    public static Polygon getDefensiveReach(Vector3 defender, Vector2 goalCenter) {
        double extendedDistance = 20;

        Vector2 flatCar = defender.flatten();
        Vector2 topPost = new Vector2(goalCenter.getX() + Goal.EXTENT, goalCenter.getY());
        Vector2 bottomPost = new Vector2(goalCenter.getX() - Goal.EXTENT, goalCenter.getY());

        double topSlope = (flatCar.getY() - topPost.getY()) / (flatCar.getX() - topPost.getX());
        double bottomSlope = (flatCar.getY() - bottomPost.getY()) / (flatCar.getX() - bottomPost.getX());
        double topFactor = extendedDistance / Math.sqrt(1 + Math.pow(topSlope, 2));
        double bottomFactor = extendedDistance / Math.sqrt(1 + Math.pow(bottomSlope, 2));

        Vector2 extendedTop = new Vector2(
                topPost.getX() + topFactor,
                topPost.getY() + (topFactor * topSlope)
        );
        Vector2 extendedBottom = new Vector2(
                bottomPost.getX() - bottomFactor,
                bottomPost.getY() - (bottomFactor * bottomSlope)
        );
        Vector2 topBox = new Vector2(extendedTop.getX(), Math.signum(defender.getY()) * 70);
        Vector2 bottomBox = new Vector2(extendedBottom.getX(), Math.signum(defender.getY()) * 70);

        return new Polygon(new Vector2[] {
                defender.flatten(),
                topPost,
                extendedTop,
                topBox,
                bottomBox,
                extendedBottom,
                bottomPost
        });
    }
}
