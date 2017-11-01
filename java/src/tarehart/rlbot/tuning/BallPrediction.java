package tarehart.rlbot.tuning;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;

import java.time.LocalDateTime;

public class BallPrediction {

    public LocalDateTime predictedMoment;
    public Vector3 predictedLocation;
    public BallPath associatedPath;

    public BallPrediction(Vector3 predictedLocation, LocalDateTime predictedMoment, BallPath ballPath) {
        this.predictedLocation = predictedLocation;
        this.predictedMoment = predictedMoment;
        this.associatedPath = ballPath;
    }

}
