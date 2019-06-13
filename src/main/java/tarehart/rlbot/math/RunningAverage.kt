package tarehart.rlbot.math

class RunningAverage(val maxSamples: Int = 100) {
    var numSamples = 0
    var average = 0.0

    fun takeSample(value: Double) {
        if (numSamples < maxSamples) {
            numSamples++
        }
        average = (value + average * (numSamples - 1)) / numSamples
    }

}
