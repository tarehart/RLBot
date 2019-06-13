package tarehart.rlbot

const val DEFAULT_PORT = 22868

fun main(args: Array<String>) {
    val port = readPortFromArgs(args) ?: DEFAULT_PORT
    BotHouse().start(port)
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
