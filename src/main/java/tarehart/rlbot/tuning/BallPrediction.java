package tarehart.rlbot.tuning;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.time.GameTime;

public class BallPrediction {

    public GameTime predictedMoment;
    public Vector3 predictedLocation;
    public BallPath associatedPath;

    public BallPrediction(Vector3 predictedLocation, GameTime predictedMoment, BallPath ballPath) {
        this.predictedLocation = predictedLocation;
        this.predictedMoment = predictedMoment;
        this.associatedPath = ballPath;
    }

}
