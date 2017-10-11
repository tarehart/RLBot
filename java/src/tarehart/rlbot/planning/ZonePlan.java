package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.tuning.BotLog;

public class ZonePlan {

    public Vector3 ballPosition;
    public CarData myCar;
    public CarData opponentCar;
    public Zone ballZone;
    public Zone myZone; // DON'T LET ME IN MY ZONE
    public Zone opponentZone;
    public boolean goForKickoff;


    public ZonePlan(AgentInput input) {
        // Store data for later use and for telemetry output
        this.ballPosition = input.ballPosition;
        if(input.team == Bot.Team.BLUE) {
            this.myCar = input.blueCar;
            this.opponentCar = input.orangeCar;
        }
        else {
            this.myCar = input.orangeCar;
            this.opponentCar = input.blueCar;
        }

        determineZonePlan(input.team);
    }


    //TODO: Actually generate some recommended steps from this
    private void determineZonePlan(Bot.Team team) {
        ballZone = new Zone(getMainZone(ballPosition), getSubZone(ballPosition));
        myZone = new Zone(getMainZone(myCar.position), getSubZone(myCar.position));
        opponentZone = new Zone(getMainZone(opponentCar.position), getSubZone(opponentCar.position));

        if(!isAnalysisSane(ballZone, myZone, opponentZone, team))
            return; // Don't even bother coming up with a plan in this case

        // Determine if this bot should go for kickoff or avoid it
        if(team == Bot.Team.BLUE)
            goForKickoff = this.myZone.mainZone == Zone.MainZone.BLUE;
        else
            goForKickoff = this.myZone.mainZone == Zone.MainZone.ORANGE;
        //TODO: Come up with a plan
    }

    private Zone.MainZone getMainZone(Vector3 point) {
        Vector2 flatPoint = point.flatten();
        if(ZoneDefinitions.ORANGE.contains(flatPoint))
            return Zone.MainZone.ORANGE;
        if(ZoneDefinitions.MID.contains(flatPoint))
            return Zone.MainZone.MID;
        if(ZoneDefinitions.BLUE.contains(flatPoint))
            return Zone.MainZone.BLUE;
        return Zone.MainZone.NONE;
    }

    private Zone.SubZone getSubZone(Vector3 point) {
        Vector2 flatPoint = point.flatten();
        if(ZoneDefinitions.TOPCORNER.contains(flatPoint))
            return Zone.SubZone.TOPCORNER;
        if(ZoneDefinitions.BOTTOMCORNER.contains(flatPoint))
            return Zone.SubZone.BOTTOMCORNER;
        if(ZoneDefinitions.TOP.contains(flatPoint))
            return Zone.SubZone.TOP;
        if(ZoneDefinitions.BOTTOM.contains(flatPoint))
            return Zone.SubZone.BOTTOM;
        return Zone.SubZone.NONE;
    }

    private boolean isAnalysisSane(Zone ballZone, Zone myZone, Zone opponentZone, Bot.Team team) {
        boolean sanityCheck = true;
        if(ballZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where is the ball?", team);
            sanityCheck = false;
        }
        if(myZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where is the ball?", team);
            sanityCheck = false;
        }
        if(opponentZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where is the ball?", team);
            sanityCheck = false;
        }

        return sanityCheck;
    }
}
