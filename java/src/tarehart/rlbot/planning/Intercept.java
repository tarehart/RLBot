package tarehart.rlbot.planning;

import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.time.GameTime;

public class Intercept {
    private Vector3 space;
    private GameTime time;
    private double airBoost;
    private StrikeProfile strikeProfile;
    private DistancePlot distancePlot;

    public Intercept(Vector3 space, GameTime time, double airBoost, StrikeProfile strikeProfile, DistancePlot distancePlot) {
        this.space = space;
        this.time = time;
        this.airBoost = airBoost;
        this.strikeProfile = strikeProfile;
        this.distancePlot = distancePlot;
    }

    public Intercept(SpaceTime spaceTime, StrikeProfile strikeProfile, DistancePlot distancePlot) {
        this(spaceTime.space, spaceTime.time, 0, strikeProfile, distancePlot);
    }

    public double getAirBoost() {
        return airBoost;
    }

    public Vector3 getSpace() {
        return space;
    }

    public GameTime getTime() {
        return time;
    }

    public SpaceTime toSpaceTime() {
        return new SpaceTime(space, time);
    }

    public StrikeProfile getStrikeProfile() {
        return strikeProfile;
    }

    public DistancePlot getDistancePlot() {
        return distancePlot;
    }
}
