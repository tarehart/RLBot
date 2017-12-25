package tarehart.rlbot.carpredict;

import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.GameTime;

public class CarSlice {
    public final Vector3 space;
    public final GameTime time;
    public final Vector3 velocity;
    public final CarOrientation orientation;

    public CarSlice(Vector3 space, GameTime time, Vector3 velocity, CarOrientation orientation) {
        this.space = space;
        this.time = time;
        this.velocity = velocity;
        this.orientation = orientation;
    }

    public Vector3 getSpace() {
        return space;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public GameTime getTime() {
        return time;
    }

    public SpaceTime toSpaceTime() {
        return new SpaceTime(space, time);
    }

    @Override
    public String toString() {
        return "CarSlice{" +
                "space=" + space +
                ", time=" + time +
                ", velocity=" + velocity +
                ", orientation=" + orientation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CarSlice carSlice = (CarSlice) o;

        if (space != null ? !space.equals(carSlice.space) : carSlice.space != null) return false;
        if (time != null ? !time.equals(carSlice.time) : carSlice.time != null) return false;
        if (velocity != null ? !velocity.equals(carSlice.velocity) : carSlice.velocity != null) return false;
        return orientation != null ? orientation.equals(carSlice.orientation) : carSlice.orientation == null;
    }

    @Override
    public int hashCode() {
        int result = space != null ? space.hashCode() : 0;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (velocity != null ? velocity.hashCode() : 0);
        result = 31 * result + (orientation != null ? orientation.hashCode() : 0);
        return result;
    }
}
