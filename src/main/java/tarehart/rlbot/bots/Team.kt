package tarehart.rlbot.bots

enum class Team {
    BLUE,
    ORANGE;

    fun opposite(): Team {
        return if (this == BLUE) ORANGE else BLUE
    }
}