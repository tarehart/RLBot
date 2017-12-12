package tarehart.rlbot.input;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.GameTime;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FullBoost {

    private static final float MIDFIELD_BOOST_WIDTH = 71.5f;
    private static final float CORNER_BOOST_WIDTH = 61.5f;
    private static final float CORNER_BOOST_DEPTH = 82;

    private static final List<Vector3> boostLocations = Arrays.asList(
            new Vector3(MIDFIELD_BOOST_WIDTH, 0, 0),
            new Vector3(-MIDFIELD_BOOST_WIDTH, 0, 0),
            new Vector3(-CORNER_BOOST_WIDTH, -CORNER_BOOST_DEPTH, 0),
            new Vector3(-CORNER_BOOST_WIDTH, CORNER_BOOST_DEPTH, 0),
            new Vector3(CORNER_BOOST_WIDTH, -CORNER_BOOST_DEPTH, 0),
            new Vector3(CORNER_BOOST_WIDTH, CORNER_BOOST_DEPTH, 0)
    );

    public Vector3 location;
    public boolean isActive;
    public GameTime activeTime;

    public FullBoost(Vector3 location, boolean isActive, GameTime activeTime) {
        this.location = location;
        this.isActive = isActive;
        this.activeTime = activeTime;
    }

    public static Optional<Vector3> getFullBoostLocation(Vector3 location) {
        for (Vector3 boostLoc: boostLocations) {
            if (boostLoc.distance(location) < 2) {
                return Optional.of(boostLoc);
            }
        }
        return Optional.empty();
    }

}
