package tarehart.rlbot.hoops

import rlbot.cppinterop.RLBotDll
import java.util.function.Consumer

class HoopsManager {

    private var isHoops = false

    fun isHoopsMode() : Boolean {

        if (isHoops) {
            return true
        }

        synchronized (this) {
            val fieldInfo = RLBotDll.getFieldInfo()
            if (fieldInfo.boostPadsLength() == 20) {
                isHoops = true
            }
        }

        return isHoops
    }
}