package tarehart.rlbot

import io.grpc.stub.StreamObserver
import rlbot.api.BotGrpc
import rlbot.api.GameData
import tarehart.rlbot.bots.*
import tarehart.rlbot.input.Chronometer
import tarehart.rlbot.input.SpinTracker
import tarehart.rlbot.ui.StatusSummary
import java.util.concurrent.ConcurrentHashMap

class GrpcService(private val statusSummary: StatusSummary) : BotGrpc.BotImplBase() {
    private val bots = ConcurrentHashMap<Int, Bot>()
    private val chronometer = Chronometer()
    private val spinTracker = SpinTracker()

    private var frameCount: Long = 0

    override fun getControllerState(request: GameData.GameTickPacket, responseObserver: StreamObserver<GameData.ControllerState>) {
        responseObserver.onNext(doGetControllerState(request))
        responseObserver.onCompleted()
    }

    private fun doGetControllerState(request: GameData.GameTickPacket): GameData.ControllerState {


        try {
            val playerIndex = request.playerIndex

            // Do nothing if we know nothing about our car
            if (request.playersCount <= playerIndex) {
                return AgentOutput().toControllerState()
            }

            val translatedInput = AgentInput(request, playerIndex, chronometer, spinTracker, frameCount++)

            val bot = bots.getOrPut(playerIndex, { initNewBot(translatedInput, playerIndex) })

            return bot.processInput(translatedInput).toControllerState()
        } catch (e: Exception) {
            e.printStackTrace()
            return AgentOutput().toControllerState()
        }

    }

    private fun initNewBot(translatedInput: AgentInput, playerIndex: Int) : Bot {
        val newBot: Bot
        if (translatedInput.myCarData.name.startsWith("JumpingBean")) {
            newBot = JumpingBeanBot(translatedInput.team, playerIndex)
        } else if (translatedInput.myCarData.name.startsWith("AdversityBot")) {
            newBot = AdversityBot(translatedInput.team, playerIndex)
        } else if (translatedInput.myCarData.name.startsWith("Air Bud")) {
            newBot = AirBudBot(translatedInput.team, playerIndex)
        } else {
            newBot = ReliefBot(translatedInput.team, playerIndex)
        }
        statusSummary.markTeamRunning(translatedInput.team, playerIndex, newBot.debugWindow)

        return newBot
    }
}
