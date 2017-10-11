package tarehart.rlbot.planning;

import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;

public class ZoneDefinitions {

    //NOTE: Zone boundaries use some rounded values that go outside of the field since that won't
    // have any negative effect
    public static final Polygon ORANGE = new Polygon(new Vector2[]{
        new Vector2(-82, 70),
        new Vector2(82, 70),
        new Vector2(82, 120), //TODO: Check if this is far enough back to cover the back of the net
        new Vector2(-82, 120),
        new Vector2(-82, 70)
    });
    public static final Polygon MID = new Polygon(new Vector2[] {
        new Vector2(-82, -70),
        new Vector2(82, -70),
        new Vector2(82, 70),
        new Vector2(-82, 70),
        new Vector2(-82, -70)
    });
    public static final Polygon BLUE = new Polygon(new Vector2[] {
        new Vector2(-82, -70),
        new Vector2(82, -70),
        new Vector2(82, -120), //TODO: Check if this is far enough back to cover the back of the net
        new Vector2(-82, -120),
        new Vector2(-82, -70)
    });
    public static final Polygon TOP = new Polygon(new Vector2[] {
        new Vector2(0, -120), //TODO: Check if this is far enough back to cover the back of the net
        new Vector2(-82, -120),
        new Vector2(-82, 120),
        new Vector2(0, 120),
        new Vector2(0, -120)
    });
    public static final Polygon BOTTOM = new Polygon(new Vector2[] {
        new Vector2(0, -120), //TODO: Check if this is far enough back to cover the back of the net
        new Vector2(82, -120),
        new Vector2(82, 120),
        new Vector2(0, 120),
        new Vector2(0, -120)
    });
    public static final Polygon TOPCORNER = new Polygon(new Vector2[] {
        new Vector2(-82, -70),
        new Vector2(-50, -102.1),
        new Vector2(-50, -120),//TODO: Check if this is far enough back to cover the back of the net
        new Vector2(-82, -120),
        new Vector2(-82, -70),
        new Vector2(-82, 70),
        new Vector2(-50, 102.1),
        new Vector2(-50, 120),//TODO: Check if this is far enough back to cover the back of the net
        new Vector2(-82, 120),
        new Vector2(-82, 70),
        new Vector2(-82, -70)
    });
    public static final Polygon BOTTOMCORNER = new Polygon(new Vector2[] {
        new Vector2(82, -70),
        new Vector2(50, -102.1),
        new Vector2(50, -120),//TODO: Check if this is far enough back to cover the back of the net
        new Vector2(82, -120),
        new Vector2(82, -70),
        new Vector2(82, 70),
        new Vector2(50, 102.1),
        new Vector2(50, 120),//TODO: Check if this is far enough back to cover the back of the net
        new Vector2(82, 120),
        new Vector2(82, 70),
        new Vector2(82, -70)
    });
}
