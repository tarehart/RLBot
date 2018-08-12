package tarehart.rlbot.intercept

open class LaunchChecklist {
    var linedUp: Boolean = false
    var closeEnough: Boolean = false
    var timeForIgnition: Boolean = false
    var upright: Boolean = false
    var onTheGround: Boolean = false

    open fun readyToLaunch(): Boolean {
        return linedUp && closeEnough && timeForIgnition && upright && onTheGround
    }
}
