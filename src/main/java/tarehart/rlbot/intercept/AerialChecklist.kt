package tarehart.rlbot.intercept

class AerialChecklist : LaunchChecklist() {
    var notSkidding: Boolean = false
    var hasBoost: Boolean = false

    override fun readyToLaunch(): Boolean {
        return hasBoost && notSkidding && super.readyToLaunch()
    }
}
