package tarehart.rlbot.physics.cpp

import rlbot.cppinterop.RLBotDll
import tarehart.rlbot.physics.BallPath


object BallPredictorHelper {

    fun predictPath(): BallPath {
        val ballPrediction = RLBotDll.getBallPrediction()
        return BallPath(ballPrediction)
    }
}
