package tarehart.rlbot.tuning

import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.GameTime

data class BallPrediction(
        val predictedLocation: Vector3,
        val predictedMoment: GameTime,
        val associatedPath: BallPath)
