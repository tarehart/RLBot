package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.tuning.BotLog;

import java.util.Optional;

public class ZonePlan {

    public Vector3 ballPosition;
    public CarData myCar;
    public Optional<CarData> opponentCar;
    public Zone ballZone;
    public Zone myZone; // DON'T LET ME IN MY ZONE
    public Optional<Zone> opponentZone;
    public boolean ballIsInMyBox;
    public boolean ballIsInOpponentBox;



    public ZonePlan(AgentInput input) {
        // Store data for later use and for telemetry output
        ballPosition = input.ballPosition;
        myCar = input.getMyCarData();
        opponentCar = input.getEnemyCarData();

        determineZonePlan(input.team);
    }


    //TODO: Actually generate some recommended steps from this
    private void determineZonePlan(Bot.Team team) {
        ballZone = new Zone(getMainZone(ballPosition), getSubZone(ballPosition));
        myZone = new Zone(getMainZone(myCar.position), getSubZone(myCar.position));
        opponentZone = opponentCar.map(c -> new Zone(getMainZone(c.position), getSubZone(c.position)));

        if(!isAnalysisSane(ballZone, myZone, opponentZone, team))
            return; // Don't even bother coming up with a plan in this case

        // Determine if the ball is in my our their box
        if(team == Bot.Team.BLUE) {
            ballIsInMyBox = this.ballZone.subZone == Zone.SubZone.BLUEBOX;
            ballIsInOpponentBox = this.ballZone.subZone == Zone.SubZone.ORANGEBOX;
        }
        else {
            ballIsInMyBox = this.ballZone.subZone == Zone.SubZone.ORANGEBOX;
            ballIsInOpponentBox = this.ballZone.subZone == Zone.SubZone.BLUEBOX;
        }

        //TODO: Come up with a plan
    }

    // The order of analysis of zones basically determines their priority
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

    // The order of analysis of sub zones basically determines their priority
    private Zone.SubZone getSubZone(Vector3 point) {
        Vector2 flatPoint = point.flatten();
        if(ZoneDefinitions.TOPCORNER.contains(flatPoint))
            return Zone.SubZone.TOPCORNER;
        if(ZoneDefinitions.BOTTOMCORNER.contains(flatPoint))
            return Zone.SubZone.BOTTOMCORNER;
        if(ZoneDefinitions.ORANGEBOX.contains(flatPoint))
            return Zone.SubZone.ORANGEBOX;
        if(ZoneDefinitions.BLUEBOX.contains(flatPoint))
            return Zone.SubZone.BLUEBOX;
        if(ZoneDefinitions.TOP.contains(flatPoint))
            return Zone.SubZone.TOP;
        if(ZoneDefinitions.BOTTOM.contains(flatPoint))
            return Zone.SubZone.BOTTOM;
        return Zone.SubZone.NONE;
    }

    private boolean isAnalysisSane(Zone ballZone, Zone myZone, Optional<Zone> opponentZone, Bot.Team team) {
        boolean sanityCheck = true;
        if(ballZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where is the ball?", team);
            sanityCheck = false;
        }
        if(myZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where am I?", team);
            sanityCheck = false;
        }
        if(!opponentZone.isPresent() || opponentZone.get().mainZone == Zone.MainZone.NONE) {
            // BotLog.println("WTF where is the enemy?", team);
            sanityCheck = false;
        }

        return sanityCheck;
    }
}
