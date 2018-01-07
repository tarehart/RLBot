package tarehart.rlbot.carpredict

import java.util.ArrayList

class CarPath() {

    val slices = ArrayList<CarSlice>()

    fun addSlice(slice: CarSlice) {
        slices.add(slice)
    }

}
