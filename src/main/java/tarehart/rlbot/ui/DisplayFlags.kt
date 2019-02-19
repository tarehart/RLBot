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
        flags[DETAILED_PLAN] = Integer.parseInt(props.getProperty("flags.$DETAILED_PLAN"))
        flags[BALL_PATH] = Integer.parseInt(props.getProperty("flags.$BALL_PATH"))
        flags[CAR_PATH] = Integer.parseInt(props.getProperty("flags.$CAR_PATH"))
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

    // Shows full plan details in the top left of the screen
    // Modes:
    // 0 : Hide
    // 1 : Show
    val DETAILED_PLAN = "detailedPlan"

    // Shows ball path projected in 3d
    // Modes:
    // 0 : Hide
    // 1 : Show
    val BALL_PATH = "ballPath"

    // Shows car path projected in 3d
    // Modes:
    // 0 : Hide
    // 1 : Show
    val CAR_PATH = "carPath"
}
