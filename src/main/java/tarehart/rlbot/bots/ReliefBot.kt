package tarehart.rlbot.bots

import org.rlbot.twitch.action.server.api.handler.ActionEntity
import org.rlbot.twitch.action.server.model.BotAction
import org.rlbot.twitch.action.server.model.ModelApiResponse
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.intercept.strike.AerialStrike
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.RetryableViableStepPlan
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.steps.teamwork.ShadowThePlayStep
import tarehart.rlbot.steps.travel.AchieveVelocityStep
import tarehart.rlbot.time.GameTime


class ReliefBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex), ActionEntity {
    override fun getCurrentAction(): BotAction? {
        currentPlan?.let {
            return BotAction().description(it.label ?: it.situation).actionType(it.posture.name)
        }
        return null
    }

    override fun handleActionChoice(action: BotAction?): ModelApiResponse {
        val actionType = action?.actionType ?: return ModelApiResponse().code(400).message("Null choice!")
        val plan = planFromActionType(actionType, Posture.OVERRIDE, action.data)
                ?: return ModelApiResponse().code(400).message("Action type not recognized.")
        currentPlan = plan
        return ModelApiResponse().code(200).message("I'll do that.")
    }

    private fun planFromActionType(actionType: String, posture: Posture, data: Map<String, Any>?): Plan? {
        val planCreationTime = previousBundle?.agentInput?.time ?: GameTime(0)
        return when (actionType) {
            "takeShot" -> RetryableViableStepPlan(posture, "Take Shot", ShadowThePlayStep()) {
                it.tacticalSituation.shotOnGoalAvailable || (it.agentInput.time - planCreationTime).seconds < 3
            }.withStep(FlexibleKickStep(KickAtEnemyGoal()))
            "demolishEnemy" -> data?.let { Plan(posture).withStep(DemolishEnemyStep(isAdversityBot = false, specificTarget = data["playerIndex"] as Int?)) }
            "aerial" -> previousBundle?.let { getAerialPlan(it, posture) }
            else -> null
        }
    }

    private fun getAerialPlan(bundle: TacticalBundle, posture: Posture): Plan? {
        val intercept = bundle.tacticalSituation.expectedContact.intercept ?: return null
        val toIntercept = intercept.space - bundle.agentInput.myCarData.position
        return Plan(posture)
                .withStep(AchieveVelocityStep(toIntercept.scaledToMagnitude(AccelerationModel.MEDIUM_SPEED * .7)))
                .withSteps(AerialStrike.performDoubleJumpAerial(0.2, KickAtEnemyGoal(), intercept.toSpaceTime()))
    }

    override fun getAvailableActions(): MutableList<BotAction> {
        val actions = ArrayList<BotAction>()
        previousBundle?.let {
            if (it.tacticalSituation.shotOnGoalAvailable) {
                actions.add(BotAction().actionType("takeShot").description("Take Shot"))
            }
            if (it.agentInput.myCarData.boost > 50) {
                val enemyToDemolish = DemolishEnemyStep.selectEnemyCar(it)
                if (enemyToDemolish != null) {
                    actions.add(BotAction()
                            .actionType("demolishEnemy")
                            .description("Demolish ${enemyToDemolish.name}")
                            .putDataItem("playerIndex", enemyToDemolish.playerIndex))
                }
                actions.add(BotAction().actionType("aerial").description("Start an aerial"))
            }
        }

        return actions
    }
}
