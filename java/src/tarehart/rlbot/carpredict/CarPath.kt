package tarehart.rlbot.carpredict

import java.util.ArrayList

class CarPath() {

    val path = ArrayList<CarSlice>()

    val firstSlice
        get() = path[0]

    val lastSlice
        get() = path[path.size - 1]

    fun addSlice(slice: CarSlice) {
        path.add(slice)
    }

}
