package tarehart.rlbot.math


object Clamper {
    fun clamp(value: Number, min: Number, max: Number) : Float {
        return value.toFloat().coerceIn(min.toFloat(), max.toFloat())
    }
}
