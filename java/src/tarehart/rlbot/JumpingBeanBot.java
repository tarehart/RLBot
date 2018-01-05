package tarehart.rlbot;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.TacticsAdvisor;
import tarehart.rlbot.time.GameTime;

import java.util.Optional;

public class JumpingBeanBot extends Bot {

    private TacticsAdvisor tacticsAdvisor;

    public JumpingBeanBot(Team team, int playerIndex) {
        super(team, playerIndex);
    }

    private Plan plan;

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        if (!Plan.Companion.activePlan(plan).isPresent()) {
            plan = SetPieces.jumpSuperHigh(10);
        }

        return plan.getOutput(input).get();
    }
}
