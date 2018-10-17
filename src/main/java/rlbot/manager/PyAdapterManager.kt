package rlbot.manager

import rlbot.Bot
import rlbot.cppinterop.RLBotDll
import rlbot.flat.GameTickPacket
import tarehart.rlbot.AgentOutput
import java.util.*


class PyAdapterManager {

    private val recentPackets = LinkedList<GameTickPacket>()
    private var initialized = false
    private var shutdown = false

    private fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            Thread(Runnable {
                while (!shutdown) {
                    val packet = RLBotDll.getFlatbufferPacket()
                    val secondsElapsed = packet.gameInfo().secondsElapsed()
                    System.out.println(secondsElapsed)
                    if (recentPackets.isEmpty() || recentPackets.first().gameInfo().secondsElapsed() < secondsElapsed) {
                        recentPackets.addFirst(packet)
                    }
                    while (recentPackets.size > 3) {
                        recentPackets.removeLast()
                    }
                    Thread.sleep(15)
                }
            }).start()
        }
    }


    val pyAdapterBots = HashMap<Int, Bot>()

    fun registerPyAdapter(index: Int, botSupplier: (Int) -> Bot) {
        ensureInitialized()
        pyAdapterBots.computeIfAbsent(index, botSupplier)
    }

    fun isCorrectPacket(packet: GameTickPacket, secondsElapsed: Float): Boolean {
        return Math.abs(packet.gameInfo().secondsElapsed() - secondsElapsed) < 0.001
    }

    fun getOutput(index: Int, secondsElapsed: Float): AgentOutput? {


        if (recentPackets.isEmpty() || recentPackets.first().gameInfo().secondsElapsed() < secondsElapsed) {
            val freshPacket = RLBotDll.getFlatbufferPacket()
            System.out.println("" + freshPacket.gameInfo().secondsElapsed() + " fresh")
            recentPackets.addFirst(freshPacket)
        }

        val packet = recentPackets.firstOrNull { it -> isCorrectPacket(it, secondsElapsed) }

        if (packet == null) {
            System.out.println(String.format("Failed to find a packet with %f seconds elapsed as requested by python!",
                    secondsElapsed))
            return null
        }

        val bot = pyAdapterBots[index] ?: return null
        val renderer = BotLoopRenderer.forBotLoop(bot)
        renderer.startPacket()
        val controllerState = pyAdapterBots[index]?.processInput(packet) ?: return null

        renderer.finishAndSendIfDifferent()
        return controllerState as AgentOutput
    }

    fun shutdown() {
        shutdown = true
    }
}


