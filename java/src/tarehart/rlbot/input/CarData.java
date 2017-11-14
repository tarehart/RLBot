package tarehart.rlbot.input;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.Bot;

import java.time.LocalDateTime;

public class CarData {
    public final Vector3 position;
    public final Vector3 velocity;
    public final CarOrientation orientation;
    public final CarSpin spin;
    public final double boost;
    public boolean isSupersonic;
    public final Bot.Team team;
    public final int playerIndex;
    public final LocalDateTime time;
    public final long frameCount;


    public CarData(Vector3 position, Vector3 velocity, CarOrientation orientation, CarSpin spin, double boost,
                   boolean isSupersonic, Bot.Team team, int playerIndex, LocalDateTime time, long frameCount) {
        this.position = position;
        this.velocity = velocity;
        this.orientation = orientation;
        this.spin = spin;
        this.boost = boost;
        this.isSupersonic = isSupersonic;
        this.team = team;
        this.playerIndex = playerIndex;
        this.time = time;
        this.frameCount = frameCount;
    }
}
