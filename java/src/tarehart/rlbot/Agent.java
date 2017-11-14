package tarehart.rlbot;

import com.google.gson.Gson;
import rlbot.input.PyGameTickPacket;
import tarehart.rlbot.input.Chronometer;
import tarehart.rlbot.input.SpinTracker;
import tarehart.rlbot.ui.StatusSummary;

import java.util.HashMap;
import java.util.Map;

public class Agent {

    private Map<Bot.Team, Bot> bots = new HashMap<>();
    private Gson gson = new Gson();
    private Chronometer chronometer = new Chronometer();
    private SpinTracker spinTracker = new SpinTracker();
    private StatusSummary statusSummary;
    private long frameCount = 0;

    public Agent(StatusSummary statusSummary) {
        this.statusSummary = statusSummary;
    }

    public int[] getOutputVector(String packetJson, String teamString) {
        return getAgentOutput(packetJson, teamString).toPython();
    }

    private AgentOutput getAgentOutput(String packetJson, String teamString) {

        try {
            Bot.Team team = Bot.Team.valueOf(teamString.toUpperCase());

            PyGameTickPacket packet = gson.fromJson(packetJson, PyGameTickPacket.class);
            AgentInput translatedInput = new AgentInput(packet, team, chronometer, spinTracker, frameCount++);

            // Do nothing if we know nothing about our car
            if (translatedInput.getMyCarData() == null) {
                return new AgentOutput();
            }

            // Setup bot from this packet if necessary
            synchronized (this) {
                if (!bots.containsKey(team)) {
                    int playerIndex = AgentInput.teamToPlayerIndex(team);
                    ReliefBot bot = new ReliefBot(team, playerIndex);
                    bots.put(team, bot);
                    statusSummary.markTeamRunning(team, playerIndex, bot.getDebugWindow());
                }
            }

            Bot bot = bots.get(team);

            return bot.processInput(translatedInput);
        } catch (Exception e) {
            e.printStackTrace();
            return new AgentOutput();
        }
    }
}
