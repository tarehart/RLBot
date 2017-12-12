package tarehart.rlbot.input;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.GameTime;

public class BallTouch {

    public final Bot.Team team;
    public final int playerIndex;
    public final GameTime time;
    public final Vector3 position;
    public final Vector3 normal;


    public BallTouch(Bot.Team team, int playerIndex, GameTime time, Vector3 position, Vector3 normal) {
        this.team = team;
        this.playerIndex = playerIndex;
        this.time = time;
        this.position = position;
        this.normal = normal;
    }
}
