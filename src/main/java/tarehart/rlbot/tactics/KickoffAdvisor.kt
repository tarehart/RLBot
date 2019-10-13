package tarehart.rlbot.tactics

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Zone
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.defense.ThreatAssessor
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

class KickoffAdvisor {

    private val goodKickoffs = HashMap<GoForKickoffStep.KickoffType, KickoffAdvice>()
    private var adviceUnderScrutiny: KickoffAdvice? = null
    private var gradeMoment: GameTime = GameTime(0)
    private var ourScore = 0
    private var enemyScore = 0

    fun gradeKickoff(bundle: TacticalBundle) {
        val team = bundle.agentInput.team
        adviceUnderScrutiny?.let {
            if (bundle.agentInput.getTeamScore(team) > ourScore) {
                goodKickoffs[it.kickoffType] = it
                BotLog.println("Scored on the kickoff, great!", bundle.agentInput.playerIndex)
                adviceUnderScrutiny = null
                return
            }

            if (bundle.agentInput.getTeamScore(team.opposite()) > enemyScore) {
                // We got scored on due to this kickoff, ouch!
                BotLog.println("Ouch, lost the kickoff!", bundle.agentInput.playerIndex)
                adviceUnderScrutiny = null
                return
            }

            if (bundle.agentInput.time > gradeMoment) {
                if (Zone.isInOffensiveThird(bundle.zonePlan.ballZone, team) && !ThreatAssessor.getThreatReport(bundle).looksSerious()) {
                    goodKickoffs[it.kickoffType] = it
                    BotLog.println("Kickoff seems decent, let's keep it", bundle.agentInput.playerIndex)
                } else {
                    BotLog.println("Kickoff was shabby, tossing it out", bundle.agentInput.playerIndex)
                }
                adviceUnderScrutiny = null
            }
        }
    }

    fun giveAdvice(kickoffType: GoForKickoffStep.KickoffType, bundle: TacticalBundle): KickoffAdvice {

        return goodKickoffs.getOrElse(kickoffType) {
            val newAdvice = KickoffAdvice(18 + Math.random() * 4, Math.random() < 0.3, kickoffType)
            adviceUnderScrutiny = newAdvice
            gradeMoment = bundle.agentInput.time.plusSeconds(10.0)
            ourScore = bundle.agentInput.getTeamScore(bundle.agentInput.team)
            enemyScore = bundle.agentInput.getTeamScore(bundle.agentInput.team.opposite())
            newAdvice
        }
    }

}
