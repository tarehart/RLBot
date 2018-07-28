package tarehart.rlbot.physics.cpp;

class NativeBallPredictor {

    static {
        System.loadLibrary("BallPrediction");
        System.out.println("Ball prediction library loaded!");
    }

    static native byte[] predictPath(byte[] startingSlice, float minSeconds);
}