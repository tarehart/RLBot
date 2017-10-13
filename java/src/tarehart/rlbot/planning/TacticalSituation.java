package tarehart.rlbot.planning;

import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;

import java.time.LocalDateTime;
import java.util.Optional;

public class TacticalSituation {

    public double ownGoalFutureProximity;
    public double distanceBallIsBehindUs;
    public double enemyOffensiveApproachError; // If the enemy wants to shoot on our goal, how many radians away from a direct approach? Always positive.
    public SpaceTime expectedEnemyContact;
    public double distanceFromEnemyBackWall;
    public double distanceFromEnemyCorner;
    public Optional<SpaceTimeVelocity> scoredOnThreat;
    public boolean needsDefensiveClear;
    public boolean shotOnGoalAvailable;
    public boolean forceDefensivePosture;
    public boolean goForKickoff;
}
