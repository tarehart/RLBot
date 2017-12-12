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
            return myCar.position.y > opponentCar.position.y && opponentCar.position.y > ballPosition.y;
        }
        else {
            return myCar.position.y < opponentCar.position.y && opponentCar.position.y < ballPosition.y;
        }
    }

    // Checks to see if the y position order is: opponent car, my car, ball, their net
    public static boolean isMyOffensiveBreakaway(Bot.Team team, CarData myCar, CarData opponentCar, Vector3 ballPosition) {
        if(team == Bot.Team.BLUE) {
            return ballPosition.y > myCar.position.y && myCar.position.y > opponentCar.position.y;
        }
        else {
            return ballPosition.y < myCar.position.y && myCar.position.y < opponentCar.position.y;
        }
    }

    public static Polygon getShotDefenseZone(Vector3 ballPosition, Vector2 goalCenter) {
        return new Polygon(new Vector2[] {
                ballPosition.flatten(),
                new Vector2(goalCenter.x + Goal.EXTENT, goalCenter.y),
                new Vector2(goalCenter.x - Goal.EXTENT, goalCenter.y)
        });
    }

    public static Polygon getDefensiveReach(Vector3 defender, Vector2 goalCenter) {
        double extendedDistance = 20;

        Vector2 flatCar = defender.flatten();
        Vector2 topPost = new Vector2(goalCenter.x + Goal.EXTENT, goalCenter.y);
        Vector2 bottomPost = new Vector2(goalCenter.x - Goal.EXTENT, goalCenter.y);

        double topSlope = (flatCar.y - topPost.y) / (flatCar.x - topPost.x);
        double bottomSlope = (flatCar.y - bottomPost.y) / (flatCar.x - bottomPost.x);
        double topFactor = extendedDistance / Math.sqrt(1 + Math.pow(topSlope, 2));
        double bottomFactor = extendedDistance / Math.sqrt(1 + Math.pow(bottomSlope, 2));

        Vector2 extendedTop = new Vector2(
                topPost.x + topFactor,
                topPost.y + (topFactor * topSlope)
        );
        Vector2 extendedBottom = new Vector2(
                bottomPost.x - bottomFactor,
                bottomPost.y - (bottomFactor * bottomSlope)
        );
        Vector2 topBox = new Vector2(extendedTop.x, Math.signum(defender.y) * 70);
        Vector2 bottomBox = new Vector2(extendedBottom.x, Math.signum(defender.y) * 70);

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
