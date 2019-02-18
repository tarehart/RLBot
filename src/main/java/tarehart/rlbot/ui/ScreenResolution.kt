package tarehart.rlbot.ui

import java.util.*

object ScreenResolution {

    private var x = 1
    private var y = 1

    fun init() {
        val props = Properties()
        val fileIn = javaClass.getResourceAsStream("/src/reliefbot.properties")
        props.load(fileIn)
        fileIn.close()

        x = Integer.parseInt(props.getProperty("screenResolution.x"))
        y = Integer.parseInt(props.getProperty("screenResolution.y"))
    }

    fun getX() : Int {
        return x
    }

    fun getY() : Int {
        return y
    }
}