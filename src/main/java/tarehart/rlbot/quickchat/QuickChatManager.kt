package tarehart.rlbot.quickchat

import rlbot.cppinterop.RLBotDll
import tarehart.rlbot.AgentInput
import tarehart.rlbot.time.GameTime

class QuickChatManager(val playerIndex: Int, val team: Int) {

    var lastMessageIndex = -1
    val latestTeamMessages = HashMap<Int, QuickChatRecord>()

    fun receive(input: AgentInput) {
        val messages = RLBotDll.receiveQuickChat(playerIndex, team, lastMessageIndex)
        for (i in 0 until messages.messagesLength()) {
            val message = messages.messages(i)
            if (message.playerIndex() == playerIndex) {
                continue // Ignore messages coming from self
            }
            if (input.allCars[message.playerIndex()].team.ordinal == team) {
                latestTeamMessages[message.playerIndex()] = QuickChatRecord(message.quickChatSelection(), GameTime.fromGameSeconds(message.timeStamp()))
            }
            lastMessageIndex = message.messageIndex()
        }
    }

    fun hasLatestChatFromTeammate(chatSelection: Byte, moreRecentlyThan: GameTime): Boolean {
        for (chat in latestTeamMessages.values) {
            if (chat.time.isAfter(moreRecentlyThan) && chat.quickChat == chatSelection) {
                return true
            }
        }
        return false
    }
}
