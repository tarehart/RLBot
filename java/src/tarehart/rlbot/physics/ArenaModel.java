package tarehart.rlbot.physics;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.*;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.Plane;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/*
TODO: Start using a real arena mesh.
it's not that hard to get, i'm just using this tool to decrypt .upk files
https://www.reddit.com/r/RocketLeague/comments/5751g0/i_fixed_the_decryptor_tool_we_now_have_exact/
then using umodel to scroll through the models and export .psk files
http://www.gildor.org/en/projects/umodel
i'm using blender it has an addon to import psk files

-Marvin

TODO: Try using the libgdx wrapper around the bullet physics engine instead of ODE:
https://github.com/libgdx/libgdx/wiki/Bullet-physics
This gives us a much clearer slices to importing the arena model, and possibly better performance.
 */
public class ArenaModel {

    public static final float SIDE_WALL = 81.92f;
    public static final float BACK_WALL = 102.4f;
    public static final float CEILING = 40.88f;
    public static final float BALL_ANGULAR_DAMPING = 0f;

    private static final int WALL_THICKNESS = 10;
    private static final int WALL_LENGTH = 400;
    public static final float GRAVITY = 13f;
    public static final float BALL_DRAG = .0015f;
    public static final float BALL_RADIUS = 1.8555f;

    public static final double CORNER_BEVEL = 11.8; // 45 degree angle walls come in this far from where the rectangular corner would be.
    public static final Vector2 CORNER_ANGLE_CENTER = new Vector2(SIDE_WALL, BACK_WALL).minus(new Vector2(CORNER_BEVEL, CORNER_BEVEL));

    // The diagonal surfaces that merge the floor and the wall--
    // Higher = more diagonal showing.
    public static final float RAIL_HEIGHT = 2.5f;
    public static final float BALL_RESTITUTION = .6f;
    public static final int STEPS_PER_SECOND = 20;
    public static final float MOMENT_OF_INERTIA_BONUS = 1.45f;

    private DWorld world;
    private DSpace space;
    private DSphere ball;
    private final DJointGroup contactgroup;

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

    public static final Duration SIMULATION_DURATION = Duration.Companion.ofSeconds(6);
    private static final LoadingCache<BallSlice, BallPath> pathCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(new CacheLoader<BallSlice, BallPath>() {
                @Override
                public BallPath load(BallSlice key) throws Exception {
                    synchronized (lock) {
                        // Always use a new ArenaModel. There's a nasty bug
                        // where bounces stop working properly and I can't track it down.
                        return new ArenaModel().simulateBall(key, SIMULATION_DURATION);
                    }
                }
            });

    private static final Object lock = new Object();

    static {
        OdeHelper.initODE2(0);
    }

    private static double getFriction(double normalSpeed) {
        return Math.max(0, 460 * normalSpeed);
    }

    public ArenaModel() {

        world = OdeHelper.createWorld();
        space = OdeHelper.createSimpleSpace();
        world.setGravity(0, 0, -GRAVITY);
        world.setDamping(0, 0);
        setupWalls();
        ball = initBallPhysics();

        contactgroup = OdeHelper.createJointGroup();
    }

    private DSphere initBallPhysics() {

        DSphere sphere = OdeHelper.createSphere(space, BALL_RADIUS);
        DBody body = OdeHelper.createBody(world);
        DMass mass = OdeHelper.createMass();
        mass.setSphere(1, BALL_RADIUS * MOMENT_OF_INERTIA_BONUS); // Huge moment of inertia
        body.setMass(mass);
        sphere.setBody(body);
        body.setDamping(BALL_DRAG, BALL_ANGULAR_DAMPING);

        return sphere;
    }

    private DGeom.DNearCallback nearCallback = this::nearCallback;

    // this is called by dSpaceCollide when two objects in space are
    // potentially colliding.

    /**
     * https://www.ode-wiki.org/wiki/index.php?title=Manual:_Concepts#Physics_model
     * https://www.ode-wiki.org/wiki/index.php?title=Manual:_Joint_Types_and_Functions#Contact
     */
    private void nearCallback (Object data, DGeom o1, DGeom o2) {
        // only collide things with the ball
        if (!(o1 == ball || o2 == ball)) {
            return;
        }

        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();

        DContactBuffer contacts = new DContactBuffer(1);
        int numContacts = OdeHelper.collide(o1, o2,1, contacts.getGeomBuffer());
        for (int i = 0; i < numContacts; i++) {

            DContact c = contacts.get(i);

            Vector3 normal = toV3(c.getContactGeom().normal);
            Vector3 velocity = toV3(ball.getBody().getLinearVel());

            double normXMagnitude = Math.abs(normal.getX());
            boolean isRollAlongSurface = normXMagnitude > .05 && normXMagnitude < .65 || // Corner half-angle
                    normXMagnitude < .95 && normXMagnitude > .75; // Corner half-angle
                    //normal.getZ() > .7 && normal.getZ() < .72; // Floor rail

            if (normal.dotProduct(velocity) < 0) {
                // Ball has already bounced, so don't bother creating a joint.
                return;
            }

            // The depth of the contact affects the moment of inertia.
            // For example, if the ball penetrates the wall a lot, almost to a whole radius,
            // the ball won't be able to spin because there's extremely low torque.

            // To combat this, move the ball to the surface manually.
            // ball position += collision normal * depth * -1
            Vector3 positionModifier = normal.scaled(c.geom.depth * -1);
            ball.setPosition(ball.getPosition().clone().add(toV3f(positionModifier)));
            ball.getAABB(); // This forces a recompute.

            c.geom.depth = 0;
            c.surface.mode = OdeConstants.dContactBounce;
            c.surface.bounce = isRollAlongSurface ? 1 : BALL_RESTITUTION;

            Vector3 velocityAlongSurface = velocity.projectToPlane(normal);
            if (!velocityAlongSurface.isZero()) {
                c.surface.mode |= OdeConstants.dContactFDir1;
                c.fdir1.set(toV3f(velocityAlongSurface.normaliseCopy()));
            }

            Vector3 velAlongNormal = VectorUtil.INSTANCE.project(velocity, normal);
            c.surface.mu = isRollAlongSurface ? 0 : getFriction(velAlongNormal.magnitude());

            DJoint joint = OdeHelper.createContactJoint(world, contactgroup, contacts.get(i));
            joint.attach(b1, b2);
        }
    }

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

    private void setupWalls() {

        DBody arenaBody = OdeHelper.createBody(world);
        arenaBody.setKinematic();

        for (Plane p : majorUnbrokenPlanes) {
            addWallToWorld(p.getNormal(), p.getPosition(), arenaBody);
        }

        float sideOffset = (float) (WALL_LENGTH / 2 + Goal.Companion.getEXTENT());
        float heightOffset = (float) (WALL_LENGTH / 2 + Goal.Companion.getGOAL_HEIGHT());

        // Wall on the negative side
        addWallToWorld(new Vector3(0, 1, 0), new Vector3(sideOffset, -BACK_WALL, 0), arenaBody);
        addWallToWorld(new Vector3(0, 1, 0), new Vector3(-sideOffset, -BACK_WALL, 0), arenaBody);
        addWallToWorld(new Vector3(0, 1, 0), new Vector3(0, -BACK_WALL, heightOffset), arenaBody);

        // Wall on the positive side
        addWallToWorld(new Vector3(0, -1, 0), new Vector3(sideOffset, BACK_WALL, 0), arenaBody);
        addWallToWorld(new Vector3(0, -1, 0), new Vector3(-sideOffset, BACK_WALL, 0), arenaBody);
        addWallToWorld(new Vector3(0, -1, 0), new Vector3(0, BACK_WALL, heightOffset), arenaBody);

        // Corner half-angles to make the ball roll smoothly
        double halfAngleBackoff = 3.5;
        double angle = Math.PI / 8;
        double normMajor = Math.cos(angle);
        double normMinor = Math.sin(angle);

        Vector3 halfAnglePosition = CORNER_ANGLE_CENTER.plus(new Vector2(halfAngleBackoff, halfAngleBackoff)).toVector3();
        addWallToWorld(new Vector3(normMajor, normMinor, 0), halfAnglePosition.scaled(-1), arenaBody);
        addWallToWorld(new Vector3(normMinor, normMajor, 0), halfAnglePosition.scaled(-1), arenaBody);

        addWallToWorld(new Vector3(-normMajor, -normMinor, 0), halfAnglePosition, arenaBody);
        addWallToWorld(new Vector3(-normMinor, -normMajor, 0), halfAnglePosition, arenaBody);

        addWallToWorld(new Vector3(-normMajor, normMinor, 0), new Vector3(halfAnglePosition.getX(), -halfAnglePosition.getY(), 0), arenaBody);
        addWallToWorld(new Vector3(-normMinor, normMajor, 0), new Vector3(halfAnglePosition.getX(), -halfAnglePosition.getY(), 0), arenaBody);

        addWallToWorld(new Vector3(normMajor, -normMinor, 0), new Vector3(-halfAnglePosition.getX(), halfAnglePosition.getY(), 0), arenaBody);
        addWallToWorld(new Vector3(normMinor, -normMajor, 0), new Vector3(-halfAnglePosition.getX(), halfAnglePosition.getY(), 0), arenaBody);


        // 45 degree angle rails on sides
        addWallToWorld(new Vector3(1, 0, 1), new Vector3(-SIDE_WALL, 0, RAIL_HEIGHT), arenaBody);
        addWallToWorld(new Vector3(-1, 0, 1), new Vector3(SIDE_WALL, 0, RAIL_HEIGHT), arenaBody);

        // 45 degree angle rails on back walls, either side of the goal
        addWallToWorld(new Vector3(0, 1, 1), new Vector3(sideOffset, -BACK_WALL, RAIL_HEIGHT), arenaBody);
        addWallToWorld(new Vector3(0, 1, 1), new Vector3(-sideOffset, -BACK_WALL, RAIL_HEIGHT), arenaBody);
        addWallToWorld(new Vector3(0, -1, 1), new Vector3(sideOffset, BACK_WALL, RAIL_HEIGHT), arenaBody);
        addWallToWorld(new Vector3(0, -1, 1), new Vector3(-sideOffset, BACK_WALL, RAIL_HEIGHT), arenaBody);

        // Floor rails in the corners
        float normalizedVertical = (float) Math.sqrt(2);
        float normalizedFlats = .5f;
        addWallToWorld(
                new Vector3(normalizedFlats, normalizedFlats, normalizedVertical),
                new Vector3((float) -CORNER_ANGLE_CENTER.getX(), (float) -CORNER_ANGLE_CENTER.getY(), RAIL_HEIGHT), arenaBody);
        addWallToWorld(
                new Vector3(-normalizedFlats, normalizedFlats, normalizedVertical),
                new Vector3((float) CORNER_ANGLE_CENTER.getX(), (float) -CORNER_ANGLE_CENTER.getY(), RAIL_HEIGHT), arenaBody);
        addWallToWorld(
                new Vector3(normalizedFlats, -normalizedFlats, normalizedVertical),
                new Vector3((float) -CORNER_ANGLE_CENTER.getX(), (float) CORNER_ANGLE_CENTER.getY(), RAIL_HEIGHT), arenaBody);
        addWallToWorld(
                new Vector3(-normalizedFlats, -normalizedFlats, normalizedVertical),
                new Vector3((float) CORNER_ANGLE_CENTER.getX(), (float) CORNER_ANGLE_CENTER.getY(), RAIL_HEIGHT), arenaBody);

    }

    private void addWallToWorld(Vector3 normal, Vector3 position, DBody body) {

        DBox box = OdeHelper.createBox(space, WALL_LENGTH, WALL_LENGTH, WALL_THICKNESS);
        box.setBody(body);

        normal = normal.normaliseCopy();
        box.setData(normal);
        Vector3 thicknessTweak = normal.scaled(-WALL_THICKNESS / 2);

        Vector3 finalPosition = position.plus(thicknessTweak);

        box.setOffsetPosition(finalPosition.getX(), finalPosition.getY(), finalPosition.getZ());

        Vector3 straightUp = new Vector3(0, 0, 1);
        DQuaternionC quat = getRotationFrom(straightUp, normal);
        box.setOffsetQuaternion(quat);
    }

    // https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    private DQuaternionC getRotationFrom(Vector3 fromVec, Vector3 toVec) {

        if (fromVec.dotProduct(toVec) > .99999) {
            return new DQuaternion(1, 0, 0, 0);
        }

        if (fromVec.dotProduct(toVec) < -.99999) {
            if (fromVec.getZ() < 1) {
                return new DQuaternion(0, 0, 0, 1);
            } else {
                return new DQuaternion(0, 0, 1, 0);
            }
        }

        Vector3 cross = fromVec.crossProduct(toVec);
        float magnitude = (float) (Math.sqrt(fromVec.magnitudeSquared() * toVec.magnitudeSquared()) + fromVec.dotProduct(toVec));
        DQuaternion rot = new DQuaternion(magnitude, cross.getX(), cross.getY(), cross.getZ());
        rot.normalize();
        return rot;
    }

    private Vector3 getBallPosition() {
        return toV3(ball.getBody().getPosition());
    }

    private BallPath previousBallPath = null;
    public BallPath simulateBall(BallSlice start, Duration duration) {
        final BallPath prevPath = previousBallPath;
        final BallPath ballPath;
        if (prevPath != null) {
            final BallSlice prevPrediction = prevPath.getMotionAt(start.getTime());
            if (prevPrediction != null &&
                    prevPrediction.getSpace().distance(start.getSpace()) < .3 &&
                    prevPrediction.getSpace().flatten().distance(start.getSpace().flatten()) < .1 &&
                    prevPrediction.getVelocity().distance(start.getVelocity()) < .3 &&
                    prevPrediction.getVelocity().flatten().distance(start.getVelocity().flatten()) < .1) {

                ballPath = prevPath; // Previous prediction is still legit, build on top of it.
            } else {
                ballPath = new BallPath(start);
            }
        } else {
            ballPath = new BallPath(start);  // Start over from scratch
        }
        simulateBall(ballPath, start.getTime().plus(duration));
        previousBallPath = ballPath;
        return ballPath;
    }

    private void extendSimulation(BallPath ballPath, GameTime endTime) {
        simulateBall(ballPath, endTime);
    }

    private void simulateBall(BallPath ballPath, GameTime endTime) {
        BallSlice start = ballPath.getEndpoint();

        if (start.getTime().isAfter(endTime)) {
            return;
        }

        ball.getBody().setForce(0, 0, 0);
        ball.getBody().setLinearVel(toV3f(start.getVelocity()));
        ball.getBody().setAngularVel(toV3f(start.getSpin()));
        ball.getBody().setPosition(toV3f(start.getSpace()));

        // Do some simulation
        runSimulation(ballPath, start.getTime(), endTime);
    }

    private void runSimulation(BallPath ballPath, GameTime startTime, GameTime endTime) {
        GameTime simulationTime = startTime;
        Vector3 ballVel = new Vector3();

        while (simulationTime.isBefore(endTime)) {
            float stepSize = 1.0f / STEPS_PER_SECOND;

            synchronized (lock) {
                space.collide(null, nearCallback);
                world.step(stepSize);
                contactgroup.empty();
            }

            simulationTime = simulationTime.plusSeconds(stepSize);
            Vector3 ballVelocity = getBallVelocity();
            Vector3 ballSpin = getBallSpin();
            Vector3 ballPosition = getBallPosition();
            if (Math.abs(ballPosition.getY()) > BACK_WALL + BALL_RADIUS) {
                // The ball has crossed the goal plane. Freeze it in place.
                // This is handy for making the bot not give up on saves / follow through on shots.
                ball.getBody().setKinematic();
                ball.getBody().setLinearVel(0, 0, 0);
            } else {
                ballVel = ballVelocity;
            }
            ballPath.addSlice(new BallSlice(ballPosition, simulationTime, ballVel, ballSpin));
        }
    }

    private Vector3 getBallVelocity() {
        return toV3(ball.getBody().getLinearVel());
    }

    private Vector3 getBallSpin() {
        return toV3(ball.getBody().getAngularVel());
    }

    private static DVector3C toV3f(Vector3 v) {
        return new DVector3(v.getX(), v.getY(), v.getZ());
    }

    private static Vector3 toV3(DVector3C v) {
        return new Vector3(v.get0(), v.get1(), v.get2());
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
