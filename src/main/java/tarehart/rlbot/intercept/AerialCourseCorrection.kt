package tarehart.rlbot.intercept

import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.Duration

class AerialCourseCorrection(
        val correctionDirection: Vector3,
        val averageAccelerationRequired: Double,
        val targetError: Vector3,
        val timeNeededForTurn: Duration)