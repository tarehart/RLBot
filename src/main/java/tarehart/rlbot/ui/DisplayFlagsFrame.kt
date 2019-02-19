package tarehart.rlbot.ui

import javax.swing.*

class DisplayFlagsFrame(val window: DisplayFlagsWindow, title: String): JFrame() {

    init {
        this.title = title
    }

    fun updateColors() {
        window.updateColors()
    }
}