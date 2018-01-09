package tarehart.rlbot;

import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.TacticsAdvisor;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.demolition.DemolishEnemyStep;

public class DemolitionBot extends Bot {

    public DemolitionBot(Team team, int playerIndex) {
        super(team, playerIndex);
    }

    private Plan plan;

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        if (!Plan.Companion.activePlan(plan).isPresent()) {
            plan = new Plan(Plan.Posture.OVERRIDE).withStep(new GetBoostStep()).withStep(new DemolishEnemyStep());
        }

        return plan.getOutput(input).get();
    }
}
