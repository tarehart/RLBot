package tarehart.rlbot

import org.rlbot.twitch.action.server.api.RegisterApi
import org.rlbot.twitch.action.server.invoker.ApiClient
import org.rlbot.twitch.action.server.invoker.Swagger2SpringBoot
import org.rlbot.twitch.action.server.model.ActionServerRegistration
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.SpringApplication
import java.lang.Exception

const val STANDARD_TWITCH_BROKER_PORT = 7307
const val DEFAULT_PORT = 22868

fun main(args: Array<String>) {
    val port = readPortFromArgs(args) ?: DEFAULT_PORT
    BotHouse().start(port)

    Thread.sleep(1000)

    val appArgs = DefaultApplicationArguments(args)
    val serverPortValues = appArgs.getOptionValues("server.port")
    if (serverPortValues != null && serverPortValues.size == 1) {
        // This will run on a port specified by server.port in a command line argument or
        // application.properties file. See https://www.baeldung.com/spring-boot-change-port
        Thread { SpringApplication(Swagger2SpringBoot::class.java).run(*args) }.start()


        val actionServerPort = Integer.parseInt(serverPortValues[0])
        // Ping registrations to the twitch broker
        Thread { connectToTwitchBroker(actionServerPort) }.start()
    }
}

fun connectToTwitchBroker(actionServerPort: Int) {
    val apiClient = ApiClient()
    apiClient.basePath = "http://127.0.0.1:${STANDARD_TWITCH_BROKER_PORT}"
    val registerApi = RegisterApi(apiClient)
    while (true) {
        try {
            val registration = ActionServerRegistration().baseUrl("http://127.0.0.1:${actionServerPort}")
            val response = registerApi.registerActionServer(registration)
            println("Action server registration gave ${response.code} - ${response.message}")
        } catch (e: Exception) {
            // Eat the exception
        } finally {
            Thread.sleep(10000)
        }
    }
}

fun readPortFromArgs(args: Array<String>): Int? {

    if (args.isEmpty()) {
        return null
    }
    return try {
        Integer.parseInt(args[0])
    } catch (e: NumberFormatException) {
        println("Failed to get port from arguments! Will use default.")
        null
    }
}
