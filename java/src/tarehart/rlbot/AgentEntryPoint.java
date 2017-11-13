package tarehart.rlbot;

import py4j.GatewayServer;
import tarehart.rlbot.ui.StatusSummary;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * See JavaAgent.py for usage instructions
 */
public class AgentEntryPoint {


    private Agent agent;
    private static int port;
    private static StatusSummary statusSummary = new StatusSummary();

    public AgentEntryPoint() {
        agent = new Agent(statusSummary);
    }

    public Agent getAgent() {
        return agent;
    }

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Scenario: you finished your bot and submitted it to a tournament. Your opponent hard-coded the same
        // as you, and the match can't start because of the conflict. Because of this line, you can ask the
        // organizer make a file called "port.txt" in the same directory as your .jar, and put some other number in it.
        // This matches code in JavaAgent.py
        port = GrpcServer.readPortFromFile().orElse(GrpcServer.DEFAULT_PORT);

        GatewayServer gatewayServer = new GatewayServer(new AgentEntryPoint(), port);
        gatewayServer.start();
        System.out.println(String.format("Gateway server started on port %s. Listening for Rocket League data!", port));

        showStatusSummary(port);
    }

    private static void showStatusSummary(int port) {

        statusSummary.setPort(port);

        JFrame frame = new JFrame("ReliefBot");
        frame.setContentPane(statusSummary.getRootPanel());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
