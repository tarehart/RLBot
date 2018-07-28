package tarehart.rlbot.physics.cpp

import tarehart.rlbot.math.BallSlice


object NativeBallPredictor {

    init {
        System.loadLibrary("BallPrediction")
        System.out.println("Ball prediction library loaded!")
    }

    external fun predictPath(startingSlice: BallSlice, minSeconds: Float): Array<BallSlice>
}