package tarehart.rlbot.tuning

import tarehart.rlbot.time.GameTime
import java.util.*

class PredictionWarehouse {

    private val ballPredictions = LinkedList<BallPrediction>()

    fun getPredictionOfMoment(moment: GameTime): Optional<BallPrediction> {
        if (ballPredictions.isEmpty()) {
            return Optional.empty()
        }

        if (moment.isBefore(ballPredictions.first.predictedMoment)) {
            return Optional.empty()
        }


        var oldest: BallPrediction
        do {
            if (ballPredictions.isEmpty()) {
                return Optional.empty()
            }

            oldest = ballPredictions.removeFirst()
        } while (moment.isAfter(oldest.predictedMoment))

        return Optional.of(oldest)
    }

    fun addPrediction(prediction: BallPrediction) {
        ballPredictions.add(prediction)
    }
}
