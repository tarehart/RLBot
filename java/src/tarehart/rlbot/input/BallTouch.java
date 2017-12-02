package tarehart.rlbot.input;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector3;

import java.time.LocalDateTime;

public class BallTouch {

    public final Bot.Team team;
    public final int playerIndex;
    public final LocalDateTime time;
    public final Vector3 position;
    public final Vector3 normal;


    public BallTouch(Bot.Team team, int playerIndex, LocalDateTime time, Vector3 position, Vector3 normal) {
        this.team = team;
        this.playerIndex = playerIndex;
        this.time = time;
        this.position = position;
        this.normal = normal;
    }
}
