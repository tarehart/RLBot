package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.Bot
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.tuning.BotLog

import java.util.Optional

class ZonePlan(input: AgentInput) {

    val ballPosition: Vector3 = input.ballPosition
    val myCar: CarData = input.myCarData
    val ballZone: Zone
    val myZone: Zone // DON'T LET ME IN MY ZONE
    private val ballIsInMyBox: Boolean
    private val ballIsInOpponentBox: Boolean


    init {
        // Store data for later use and for telemetry output

        ballZone = Zone(getMainZone(ballPosition), getSubZone(ballPosition))
        myZone = Zone(getMainZone(myCar.position), getSubZone(myCar.position))

        if (isAnalysisSane(ballZone, myZone, myCar.playerIndex)) {

            // Determine if the ball is in my our their box
            if (myCar.team == Bot.Team.BLUE) {
                ballIsInMyBox = this.ballZone.subZone == Zone.SubZone.BLUEBOX
                ballIsInOpponentBox = this.ballZone.subZone == Zone.SubZone.ORANGEBOX
            } else {
                ballIsInMyBox = this.ballZone.subZone == Zone.SubZone.ORANGEBOX
                ballIsInOpponentBox = this.ballZone.subZone == Zone.SubZone.BLUEBOX
            }
        } else {
            ballIsInMyBox = false;
            ballIsInOpponentBox = false;
        }
    }


    //TODO: Actually generate some recommended steps from this
    private fun determineZonePlan() {


        //TODO: Come up with a plan
    }

    // The order of analysis of zones basically determines their priority
    private fun getMainZone(point: Vector3): Zone.MainZone {
        val flatPoint = point.flatten()
        if (flatPoint in ZoneDefinitions.ORANGE)
            return Zone.MainZone.ORANGE
        if (flatPoint in  ZoneDefinitions.MID)
            return Zone.MainZone.MID
        return if (flatPoint in ZoneDefinitions.BLUE) Zone.MainZone.BLUE else Zone.MainZone.NONE
    }

    // The order of analysis of sub zones basically determines their priority
    private fun getSubZone(point: Vector3): Zone.SubZone {
        val flatPoint = point.flatten()
        if (flatPoint in ZoneDefinitions.TOPCORNER)
            return Zone.SubZone.TOPCORNER
        if (flatPoint in ZoneDefinitions.BOTTOMCORNER)
            return Zone.SubZone.BOTTOMCORNER
        if (flatPoint in ZoneDefinitions.ORANGEBOX)
            return Zone.SubZone.ORANGEBOX
        if (flatPoint in ZoneDefinitions.BLUEBOX)
            return Zone.SubZone.BLUEBOX
        if (flatPoint in ZoneDefinitions.TOP)
            return Zone.SubZone.TOP
        return if (flatPoint in ZoneDefinitions.BOTTOM) Zone.SubZone.BOTTOM else Zone.SubZone.NONE
    }

    private fun isAnalysisSane(ballZone: Zone, myZone: Zone, playerIndex: Int): Boolean {
        var sanityCheck = true
        if (ballZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where is the ball?", playerIndex)
            sanityCheck = false
        }
        if (myZone.mainZone == Zone.MainZone.NONE) {
            BotLog.println("WTF where am I?", playerIndex)
            sanityCheck = false
        }

        return sanityCheck
    }
}
