package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.time.Duration

abstract class PacketAssert(val timeLimit: Duration) {

    open var status: AssertStatus = AssertStatus.PENDING
    open var message: String? = null

    abstract fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?)
}