package tarehart.rlbot.carpredict;

import java.util.ArrayList;
import java.util.List;

public class CarPath {

    ArrayList<CarSlice> path = new ArrayList<>();

    public CarPath(CarSlice start) {
        path.add(start);
    }

    public void addSlice(CarSlice slice) {
        path.add(slice);
    }

    public List<CarSlice> getSlices() {
        return path;
    }

}
