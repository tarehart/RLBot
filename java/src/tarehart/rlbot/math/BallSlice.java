package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector3;

import java.time.LocalDateTime;

public class BallSlice {
    public final Vector3 space;
    public final LocalDateTime time;
    public final Vector3 velocity;

    // This is the axis of rotation. The magnitude represents the rate.
    // If the magnitude is 2pi, then it rotates once per second.
    public final Vector3 spin;

    @Deprecated
    public BallSlice(Vector3 space, LocalDateTime time, Vector3 velocity) {
        this(space, time, velocity, new Vector3());
    }

    public BallSlice(Vector3 space, LocalDateTime time, Vector3 velocity, Vector3 spin) {
        this.space = space;
        this.time = time;
        this.velocity = velocity;
        this.spin = spin;
    }

    @Deprecated
    public BallSlice(SpaceTime spaceTime, Vector3 velocity) {
        this(spaceTime.space, spaceTime.time, velocity);
    }

    public Vector3 getSpace() {
        return space;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public SpaceTime toSpaceTime() {
        return new SpaceTime(space, time);
    }

    @Override
    public String toString() {
        return "BallSlice{" +
                "space=" + space +
                ", time=" + time +
                ", velocity=" + velocity +
                ", spin=" + spin +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BallSlice ballSlice = (BallSlice) o;

        if (!space.equals(ballSlice.space)) return false;
        if (!time.equals(ballSlice.time)) return false;
        if (!velocity.equals(ballSlice.velocity)) return false;
        return spin.equals(ballSlice.spin);
    }

    @Override
    public int hashCode() {
        int result = space.hashCode();
        result = 31 * result + time.hashCode();
        result = 31 * result + velocity.hashCode();
        result = 31 * result + spin.hashCode();
        return result;
    }
}
