package tarehart.rlbot.ui

import java.util.*

object DisplayFlags {

    private val flags = HashMap<String, Int>()

    fun init() {
        val props = Properties()
        val fileIn = javaClass.getResourceAsStream("/src/reliefbot.properties")
        props.load(fileIn)
        fileIn.close()
    }

    operator fun set(flag: String, value: Int) {
        flags[flag] = value
    }

    operator fun get(flag: String): Int? {
        return flags[flag]
    }
}
