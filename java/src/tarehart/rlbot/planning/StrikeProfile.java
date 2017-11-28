package tarehart.rlbot.planning;

public class StrikeProfile {
    /**
     * the extra approach time added by final maneuvers before striking the ball
     */
    public double maneuverSeconds;

    /**
     * The amount of speed potentially gained over the course of the strike's final stage (generally after driving over and lining up)
     */
    public double speedBoost;

    /**
     * The amount of time spent speeding up during the final stage
     */
    public double dodgeSeconds;


    public StrikeProfile(double maneuverSeconds, double speedBoost, double dodgeSeconds) {
        this.maneuverSeconds = maneuverSeconds;
        this.speedBoost = speedBoost;
        this.dodgeSeconds = dodgeSeconds;
    }
}
