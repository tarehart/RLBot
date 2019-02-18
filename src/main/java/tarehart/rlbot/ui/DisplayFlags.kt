package tarehart.rlbot.ui

import java.util.*

object DisplayFlags {

    private val flags = HashMap<String, Int>()

    fun init() {
        val props = Properties()
        val fileIn = javaClass.getResourceAsStream("/src/reliefbot.properties")
        props.load(fileIn)
        fileIn.close()

        flags[SIMPLE_PLAN] = Integer.parseInt(props.getProperty("flags.$SIMPLE_PLAN"))
    }

    operator fun set(flag: String, value: Int) {
        flags[flag] = value
    }

    operator fun get(flag: String): Int? {
        return flags[flag]
    }

    // Shows posture and short situation near the middle of the screen
    // Modes:
    // 0 : Hide
    // 1 : Show
    val SIMPLE_PLAN = "simplePlan"
}
