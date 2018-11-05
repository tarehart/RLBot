package tarehart.rlbot.dropshot

import rlbot.cppinterop.RLBotDll

class DropshotTileManager {

    private var isDropshot = false

    fun isDropshotMode() : Boolean {

        if (isDropshot) {
            return true
        }

        synchronized (this) {
            val fieldInfo = RLBotDll.getFieldInfo()
            if (fieldInfo.goalsLength() > 10) {
                isDropshot = true
            }
        }

        return isDropshot
    }
}
