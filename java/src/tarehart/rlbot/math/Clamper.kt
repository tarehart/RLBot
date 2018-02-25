package tarehart.rlbot.math


object Clamper {
    fun clamp(value: Double, min: Double, max: Double) : Double {
        return Math.min(max, Math.max(min, value))
    }
}