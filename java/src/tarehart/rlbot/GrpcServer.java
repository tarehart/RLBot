package tarehart.rlbot;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import py4j.GatewayServer;
import tarehart.rlbot.ui.StatusSummary;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class GrpcServer {

    static final int DEFAULT_PORT = 25368;
    private static int port;
    private final Server server;
    private static StatusSummary statusSummary = new StatusSummary();

    private GrpcServer() throws IOException {
        server = ServerBuilder.forPort(port).addService(new GrpcService(statusSummary)).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            GrpcServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main method.  This comment makes the linter happy.
     */
    public static void main(String[] args) throws Exception {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Scenario: you finished your bot and submitted it to a tournament. Your opponent hard-coded the same
        // as you, and the match can't start because of the conflict. Because of this line, you can ask the
        // organizer make a file called "port.txt" in the same directory as your .jar, and put some other number in it.
        // This matches code in JavaAgent.py
        port = readPortFromFile().orElse(DEFAULT_PORT);

        GrpcServer server = new GrpcServer();
        server.start();

        System.out.println(String.format("Grpc server started on port %s. Listening for Rocket League data!", port));

        showStatusSummary(port);
        server.blockUntilShutdown();
    }

    public static Optional<Integer> readPortFromFile() {
        try {
            Stream<String> lines = Files.lines(Paths.get("port.txt"));
            Optional<String> firstLine = lines.findFirst();
            return firstLine.map(Integer::parseInt);
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse port file! Will proceed with hard-coded port number.");
            return Optional.empty();
        } catch (Throwable e) {
            return Optional.empty();
        }
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
