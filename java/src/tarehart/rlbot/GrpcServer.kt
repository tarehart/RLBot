package tarehart.rlbot

import io.grpc.ServerBuilder
import tarehart.rlbot.ui.StatusSummary
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

private const val DEFAULT_PORT = 25368
private val statusSummary = StatusSummary()

fun main(args: Array<String>) {

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Scenario: you finished your bot and submitted it to a tournament. Your opponent hard-coded the same
    // as you, and the match can't start because of the conflict. Because of this line, you can ask the
    // organizer make a file called "port.txt" in the same directory as your .jar, and put some other number in it.
    // This matches code in JavaAgent.py
    val port = readPortFromFile().orElse(DEFAULT_PORT)
    val server = ServerBuilder.forPort(port).addService(GrpcService(statusSummary)).build()
    server.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        // Use stderr here since the logger may has been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down")
        server.shutdown()
        System.err.println("*** server shut down")
    })

    println(String.format("Grpc server started on port %s. Listening for Rocket League data!", port))

    showStatusSummary(port)
    server.awaitTermination()
}


private fun readPortFromFile(): Optional<Int> {
    try {
        val lines = Files.lines(Paths.get("port.txt"))
        val firstLine = lines.findFirst()
        return firstLine.map{ Integer.parseInt(it) }
    } catch (e: NumberFormatException) {
        println("Failed to parse port file! Will proceed with hard-coded port number.")
        return Optional.empty()
    } catch (e: Throwable) {
        return Optional.empty()
    }

}

private fun showStatusSummary(port: Int) {

    statusSummary.setPort(port)

    val frame = JFrame("ReliefBot")
    frame.contentPane = statusSummary.rootPanel
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.isVisible = true
}
