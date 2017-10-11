package tarehart.rlbot.planning;

public class Zone {
    public MainZone mainZone;
    public SubZone subZone;

    public Zone(MainZone mainZone, SubZone subZone)
    {
        this.mainZone = mainZone;
        this.subZone = subZone;
    }

    public enum MainZone {
        NONE(-100),
        ORANGE(-1),
        MID(0),
        BLUE(1);

        private int mainZoneId;

        MainZone(int mainZoneId) {
            this.mainZoneId = mainZoneId;
        }
    }

    public enum SubZone {
        NONE,
        TOP,
        BOTTOM,
        TOPCORNER,
        BOTTOMCORNER;
    }
}
