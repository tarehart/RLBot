package tarehart.rlbot.bots

import tarehart.rlbot.math.vector.Vector3

enum class Team {
    BLUE,
    ORANGE;

    fun opposite(): Team {
        return if (this == BLUE) ORANGE else BLUE
    }

    val side: Int
        get() = if (this == BLUE) -1 else 1

    fun ownsPosition(position: Vector3): Boolean {
        return position.y * side > 0
    }
}
