package tarehart.rlbot.physics;

import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.Plane;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class BallPath {

    ArrayList<BallSlice> path = new ArrayList<>();

    public BallPath(BallSlice start) {
        path.add(start);
    }

    public void addSlice(BallSlice spaceTime) {
        path.add(spaceTime);
    }

    public List<BallSlice> getSlices() {
        return path;
    }

    public Optional<BallSlice> getMotionAt(GameTime time) {
        if (time.isBefore(path.get(0).getTime()) || time.isAfter(path.get(path.size() - 1).getTime())) {
            return Optional.empty();
        }

        for (int i = 0; i < path.size() - 1; i++) {
            BallSlice current = path.get(i);
            BallSlice next = path.get(i + 1);
            if (next.getTime().isAfter(time)) {

                long simulationStepMillis = Duration.between(current.getTime(), next.getTime()).toMillis();
                double tweenPoint = Duration.between(current.getTime(), time).toMillis() * 1.0 / simulationStepMillis;
                Vector3 toNext = next.getSpace().minus(current.getSpace());
                Vector3 toTween = toNext.scaled(tweenPoint);
                Vector3 space = current.getSpace().plus(toTween);
                Vector3 velocity = averageVectors(current.getVelocity(), next.getVelocity(), 1 - tweenPoint);
                return Optional.of(new BallSlice(space, time, velocity, next.spin));
            }
        }

        return Optional.of(getEndpoint());
    }

    private Vector3 averageVectors(Vector3 a, Vector3 b, double weightOfA) {
        Vector3 weightedA = a.scaled(weightOfA);
        Vector3 weightedB = b.scaled(1 - weightOfA);
        return weightedA.plus(weightedB);
    }

    /**
     * Bounce counting starts at 1.
     *
     * 0 is not a valid input.
     */
    public Optional<BallSlice> getMotionAfterWallBounce(int targetBounce) {

        assert targetBounce > 0;

        Vector3 previousVelocity = null;
        int numBounces = 0;

        for (int i = 1; i < path.size(); i++) {
            BallSlice spt = path.get(i);
            BallSlice previous = path.get(i - 1);

            if (isWallBounce(previous.getVelocity(), spt.getVelocity())) {
                numBounces++;
            }

            if (numBounces == targetBounce) {
                if (path.size() == i + 1) {
                    return Optional.empty();
                }
                return Optional.of(spt);
            }
        }

        return Optional.empty();
    }

    private boolean isWallBounce(Vector3 previousVelocity, Vector3 currentVelocity) {
        if (currentVelocity.magnitudeSquared() < .01) {
            return false;
        }
        Vector2 prev = previousVelocity.flatten();
        Vector2 curr = currentVelocity.flatten();

        return Vector2.Companion.angle(prev, curr) > Math.PI / 6;
    }

    private boolean isFloorBounce(Vector3 previousVelocity, Vector3 currentVelocity) {
        return previousVelocity.getZ() < 0 && currentVelocity.getZ() > 0;
    }

    public BallSlice getStartPoint() {
        return path.get(0);
    }

    public BallSlice getEndpoint() {
        return path.get(path.size() - 1);
    }

    public Optional<BallSlice> getLanding(GameTime startOfSearch) {

        for (int i = 1; i < path.size(); i++) {
            BallSlice spt = path.get(i);

            if (spt.getTime().isBefore(startOfSearch)) {
                continue;
            }

            BallSlice previous = path.get(i - 1);


            if (isFloorBounce(previous.getVelocity(), spt.getVelocity())) {
                if (path.size() == i + 1) {
                    return Optional.empty();
                }

                double floorGapOfPrev = previous.getSpace().getZ() - ArenaModel.BALL_RADIUS;
                double floorGapOfCurrent = spt.getSpace().getZ() - ArenaModel.BALL_RADIUS;

                BallSlice bouncePosition = new BallSlice(
                        new Vector3(spt.getSpace().getX(), spt.getSpace().getY(), ArenaModel.BALL_RADIUS),
                        spt.getTime(),
                        spt.getVelocity(),
                        spt.spin);
                if (floorGapOfPrev < floorGapOfCurrent) {
                    // TODO: consider interpolating instead of just picking the more accurate.
                    bouncePosition = new BallSlice(
                            new Vector3(previous.getSpace().getX(), previous.getSpace().getY(), ArenaModel.BALL_RADIUS),
                            previous.getTime(),
                            spt.getVelocity(),
                            spt.spin);
                }

                return Optional.of(bouncePosition);
            }
        }

        return Optional.empty();
    }

    public Optional<BallSlice> getPlaneBreak(GameTime searchStart, Plane plane, boolean directionSensitive) {
        for (int i = 1; i < path.size(); i++) {
            BallSlice spt = path.get(i);

            if (spt.getTime().isBefore(searchStart)) {
                continue;
            }

            BallSlice previous = path.get(i - 1);

            if (directionSensitive && spt.getSpace().minus(previous.getSpace()).dotProduct(plane.normal) > 0) {
                // Moving the same direction as the plane normal. If we're direction sensitive, then we don't care about plane breaks in this direction.
                continue;
            }

            Optional<Vector3> planeBreak = getPlaneBreak(previous.getSpace(), spt.getSpace(), plane);

            if (planeBreak.isPresent()) {

                Vector3 breakPosition = planeBreak.get();

                double stepSeconds = Duration.between(previous.getTime(), spt.getTime()).getSeconds();
                double tweenPoint = previous.getSpace().distance(breakPosition) / previous.getSpace().distance(spt.getSpace());
                GameTime moment = previous.getTime().plusSeconds(stepSeconds * tweenPoint);
                Vector3 velocity = averageVectors(previous.getVelocity(), spt.getVelocity(), 1 - tweenPoint);
                return Optional.of(new BallSlice(breakPosition, moment, velocity, spt.spin));
            }
        }

        return Optional.empty();
    }

    private Optional<Vector3> getPlaneBreak(Vector3 start, Vector3 end, Plane plane) {
        return VectorUtil.getPlaneIntersection(plane, start, end.minus(start));
    }

    public Optional<BallSlice> findSlice(Predicate<BallSlice> decider) {
        for (int i = 1; i < path.size(); i++) {
            if (decider.test(path.get(i))) {
                return Optional.of(path.get(i));
            }
        }
        return Optional.empty();
    }

    public Optional<BallSlice> findSlice(Predicate<BallSlice> decider, GameTime timeLimit) {
        for (int i = 1; i < path.size(); i++) {
            BallSlice slice = path.get(i);
            if (slice.getTime().isAfter(timeLimit)) {
                return Optional.empty();
            }
            if (decider.test(slice)) {
                return Optional.of(slice);
            }
        }
        return Optional.empty();
    }
}
