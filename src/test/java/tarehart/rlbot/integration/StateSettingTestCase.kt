package tarehart.rlbot.integration

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.BundleListener
import tarehart.rlbot.integration.asserts.AssertStatus
import tarehart.rlbot.integration.asserts.PacketAssert
import tarehart.rlbot.time.GameTime

class StateSettingTestCase(private val initialState: GameState, val conditions: Set<PacketAssert>): BundleListener {

    private var previousBundle: TacticalBundle? = null
    var isComplete = false
    lateinit var startTime: GameTime

    fun setState() {
        RLBotDll.setGameState(initialState.buildPacket())
    }

    override fun processBundle(bundle: TacticalBundle) {

        if (!::startTime.isInitialized) {
            startTime = bundle.agentInput.time
        }

        var hasPendingAsserts = false

        for (packetAssert: PacketAssert in conditions.filter { it.status == AssertStatus.PENDING }) {

            val duration = bundle.agentInput.time - startTime
            if (packetAssert.timeLimit < duration) {
                packetAssert.status = AssertStatus.FAILED
                packetAssert.message = "Timed out!"
            } else {
                packetAssert.checkBundle(bundle, previousBundle)
            }

            hasPendingAsserts = hasPendingAsserts || packetAssert.status == AssertStatus.PENDING
            previousBundle = bundle
        }

        isComplete = !hasPendingAsserts
    }



}