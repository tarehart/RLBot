package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.time.Duration

class CarPrinterAssert(timeLimit: Duration): PacketAssert(timeLimit, false) {

    override fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?) {

        val slice = CarSlice(bundle.agentInput.myCarData)
        val upperPoint = slice.hitboxCenterWorld + slice.toNose + slice.toRoof

        println("${bundle.agentInput.time},${upperPoint.z}")
    }
}
