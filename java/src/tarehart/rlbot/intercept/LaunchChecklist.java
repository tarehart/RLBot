package tarehart.rlbot.intercept;

public class LaunchChecklist {
    public boolean linedUp;
    public boolean closeEnough;
    public boolean notTooClose;
    public boolean timeForIgnition;
    public boolean upright;
    public boolean onTheGround;

    public boolean readyToLaunch() {
        return linedUp && closeEnough && notTooClose && timeForIgnition && upright && onTheGround;
    }
}
