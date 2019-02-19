package tarehart.rlbot.ui

import java.util.*

object DisplayFlags {

    private val flags = HashMap<String, Int>()

    fun init() {
        val props = Properties()
        val fileIn = javaClass.getResourceAsStream("/src/reliefbot.properties")
        props.load(fileIn)
        fileIn.close()

        flags[ALL] = 1 // Used for keeping track of the toggle all button for toggling the display of all flags
        flags[SIMPLE_PLAN] = Integer.parseInt(props.getProperty("flags.$SIMPLE_PLAN"))
        flags[DETAILED_PLAN] = Integer.parseInt(props.getProperty("flags.$DETAILED_PLAN"))
        flags[BALL_PATH] = Integer.parseInt(props.getProperty("flags.$BALL_PATH"))
        flags[CAR_PATH] = Integer.parseInt(props.getProperty("flags.$CAR_PATH"))
        flags[DRIBBLE_INTERCEPT] = Integer.parseInt(props.getProperty("flags.$DRIBBLE_INTERCEPT"))
        flags[HOOPS_KICKOFF] = Integer.parseInt(props.getProperty("flags.$HOOPS_KICKOFF"))
        flags[HOOPS_GOAL_PREDICTION] = Integer.parseInt(props.getProperty("flags.$HOOPS_GOAL_PREDICTION"))
        flags[GOAL_CROSSING] = Integer.parseInt(props.getProperty("flags.$GOAL_CROSSING"))
    }

    operator fun set(flag: String, value: Int) {
        flags[flag] = value
    }

    operator fun get(flag: String): Int? {
        return flags[flag]
    }

    val ALL = "allFlags"

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

    // Shows dribble intercept info projected in 3d
    // Modes:
    // 0 : Hide
    // 1 : Show
    val DRIBBLE_INTERCEPT = "dribbleIntercept"

    // Shows hoops kickoff related debug info projected in 3d
    // Modes:
    // 0 : Hide
    // 1 : Show
    val HOOPS_KICKOFF = "hoopsKickoff"

    // Shows hoops goal prediction related debug info projected in 3d
    // Modes:
    // 0 : Hide
    // 1 : Show
    val HOOPS_GOAL_PREDICTION = "hoopsGoalPrediction"

    // Shows the adjusted position for crossing the goal line when driving (projected in 3d)
    // Modes:
    // 0 : Hide
    // 1 : Show
    val GOAL_CROSSING = "goalCrossing"
}
