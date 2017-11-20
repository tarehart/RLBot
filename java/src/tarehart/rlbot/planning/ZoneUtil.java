package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.input.CarData;

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
}
