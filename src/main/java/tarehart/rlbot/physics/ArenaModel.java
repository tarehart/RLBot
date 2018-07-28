package tarehart.rlbot.physics;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.Plane;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.cpp.BallPredictorHelper;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.time.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ArenaModel {

    public static final float SIDE_WALL = 81.92f;
    public static final float BACK_WALL = 102.4f;
    public static final float CEILING = 40.88f;
    public static final float GRAVITY = 13f;
    public static final float BALL_RADIUS = 1.8555f;

    public static final double CORNER_BEVEL = 11.8; // 45 degree angle walls come in this far from where the rectangular corner would be.
    public static final Vector2 CORNER_ANGLE_CENTER = new Vector2(SIDE_WALL, BACK_WALL).minus(new Vector2(CORNER_BEVEL, CORNER_BEVEL));


    private static List<Plane> majorUnbrokenPlanes = new ArrayList<>();
    private static List<Plane> backWallPlanes = new ArrayList<>();

    static {
        // Floor
        majorUnbrokenPlanes.add(new Plane(new Vector3(0, 0, 1), new Vector3(0, 0, 0)));

        // Side walls
        majorUnbrokenPlanes.add(new Plane(new Vector3(1, 0, 0), new Vector3(-SIDE_WALL, 0, 0)));
        majorUnbrokenPlanes.add(new Plane(new Vector3(-1, 0, 0), new Vector3(SIDE_WALL, 0, 0)));

        // Ceiling
        majorUnbrokenPlanes.add(new Plane(new Vector3(0, 0, -1), new Vector3(0, 0, CEILING)));

        // 45 angle corners
        majorUnbrokenPlanes.add(new Plane(new Vector3(1, 1, 0), new Vector3((float) -CORNER_ANGLE_CENTER.getX(), (float) -CORNER_ANGLE_CENTER.getY(), 0)));
        majorUnbrokenPlanes.add(new Plane(new Vector3(-1, 1, 0), new Vector3((float) CORNER_ANGLE_CENTER.getX(), (float) -CORNER_ANGLE_CENTER.getY(), 0)));
        majorUnbrokenPlanes.add(new Plane(new Vector3(1, -1, 0), new Vector3((float) -CORNER_ANGLE_CENTER.getX(), (float) CORNER_ANGLE_CENTER.getY(), 0)));
        majorUnbrokenPlanes.add(new Plane(new Vector3(-1, -1, 0), new Vector3((float) CORNER_ANGLE_CENTER.getX(), (float) CORNER_ANGLE_CENTER.getY(), 0)));


        // Do the back wall major surfaces separately to avoid duplicate planes.
        backWallPlanes.add(new Plane(new Vector3(0, 1, 0), new Vector3(0, -BACK_WALL, 0)));
        backWallPlanes.add(new Plane(new Vector3(0, -1, 0), new Vector3(0, BACK_WALL, 0)));
    }

    public static final Duration SIMULATION_DURATION = Duration.Companion.ofSeconds(5);

    private static final ArenaModel mainModel = new ArenaModel();

    private static final LoadingCache<BallSlice, BallPath> pathCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(new CacheLoader<BallSlice, BallPath>() {
                @Override
                public BallPath load(BallSlice key) throws Exception {
                    synchronized (lock) {
                        // Always use a new ArenaModel. There's a nasty bug
                        // where bounces stop working properly and I can't track it down.
                        return mainModel.simulateBall(key, SIMULATION_DURATION);
                    }
                }
            });

    private static final Object lock = new Object();

    public static boolean isInBounds(Vector2 location) {
        return isInBounds(location.toVector3(), 0);
    }

    public static boolean isInBoundsBall(Vector3 location) {
        return isInBounds(location, BALL_RADIUS);
    }

    private static boolean isInBounds(Vector3 location, double buffer) {
        return getDistanceFromWall(location) > buffer;
    }

    public static boolean isBehindGoalLine(Vector3 position) {
        return Math.abs(position.getY()) > BACK_WALL;
    }

    public static BallPath predictBallPath(AgentInput input) {
        try {
            BallSlice key = new BallSlice(input.getBallPosition(), input.getTime(), input.getBallVelocity(), input.getBallSpin());
            return pathCache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to compute ball slices!", e);
        }
    }

    private BallPath previousBallPath = null;
    public BallPath simulateBall(BallSlice start, Duration duration) {
        final BallPath prevPath = previousBallPath;
        final BallPath ballPath;
        if (prevPath != null) {
            final BallSlice prevPrediction = prevPath.getMotionAt(start.getTime());
            if (prevPath.getEndpoint().getTime().minus(start.getTime()).getSeconds() > SIMULATION_DURATION.getSeconds() &&
                    prevPrediction != null &&
                    prevPrediction.getSpace().distance(start.getSpace()) < .3 &&
                    prevPrediction.getSpace().flatten().distance(start.getSpace().flatten()) < .1 &&
                    prevPrediction.getVelocity().distance(start.getVelocity()) < .3 &&
                    prevPrediction.getVelocity().flatten().distance(start.getVelocity().flatten()) < .1) {

                ballPath = prevPath; // Previous prediction is still legit, build on top of it.
            } else {
                ballPath = BallPredictorHelper.INSTANCE.predictPath(start, (float) duration.getSeconds());
            }
        } else {
            ballPath = BallPredictorHelper.INSTANCE.predictPath(start, (float) duration.getSeconds());
        }
        previousBallPath = ballPath;
        return ballPath;
    }

    public static boolean isCarNearWall(CarData car) {
        return getDistanceFromWall(car.getPosition()) < 2;
    }

    public static double getDistanceFromWall(Vector3 position) {
        double sideWall = SIDE_WALL - Math.abs(position.getX());
        double backWall = BACK_WALL - Math.abs(position.getY());
        double diagonal = CORNER_ANGLE_CENTER.getX() + CORNER_ANGLE_CENTER.getY() - Math.abs(position.getX()) - Math.abs(position.getY());
        return Math.min(Math.min(sideWall, backWall), diagonal);
    }

    public static boolean isCarOnWall(CarData car) {
        return car.getHasWheelContact() && isCarNearWall(car) && Math.abs(car.getOrientation().getRoofVector().getZ()) < 0.05;
    }

    public static boolean isNearFloorEdge(Vector3 position) {
        return Math.abs(position.getX()) > Goal.Companion.getEXTENT() && getDistanceFromWall(position) + position.getZ() < 6;
    }

    @NotNull
    public static Plane getNearestPlane(@NotNull Vector3 position) {

        return Streams.concat(majorUnbrokenPlanes.stream(), backWallPlanes.stream()).min((p1, p2) -> {
            double p1Distance = p1.distance(position);
            double p2Distance = p2.distance(position);
            if (p1Distance > p2Distance) {
                return 1;
            }

            return -1;
        }).get();
    }

    @NotNull
    public static Plane getBouncePlane(@NotNull Vector3 origin, @NotNull Vector3 direction) {
        Vector3 longDirection = direction.scaledToMagnitude(500);

        Map<Plane, Double> intersectionDistances = Streams.concat(majorUnbrokenPlanes.stream(), backWallPlanes.stream())
                .collect(Collectors.toMap(p -> p, p -> {
                    Vector3 intersection = VectorUtil.INSTANCE.getPlaneIntersection(p, origin, longDirection);
                    if (intersection == null) {
                        return Double.MAX_VALUE;
                    }
                    return intersection.distance(origin);
                }));

        return intersectionDistances.entrySet().stream().min(Comparator.comparing(Map.Entry::getValue)).get().getKey();
    }
}
