package tarehart.rlbot.bots

import tarehart.rlbot.math.vector.Vector3

enum class Team {
    BLUE,
    ORANGE;

    fun opposite(): Team {
        return if (this == BLUE) ORANGE else BLUE
    }

    fun ownsPosition(position: Vector3): Boolean {
        val side = if (this == BLUE) -1 else 1
        return position.y * side > 0
    }
}
