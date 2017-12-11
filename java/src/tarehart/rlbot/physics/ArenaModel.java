package tarehart.rlbot.physics;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.*;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.Goal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


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

    public static final Vector2 CORNER_ANGLE_CENTER = new Vector2(70.5, 90.2);

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

    public static final Duration SIMULATION_DURATION = Duration.ofSeconds(6);
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
            c.surface.bounce = BALL_RESTITUTION;

            Vector3 velocityAlongSurface = velocity.projectToPlane(normal);
            if (!velocityAlongSurface.isZero()) {
                c.surface.mode |= OdeConstants.dContactFDir1;
                c.fdir1.set(toV3f(velocityAlongSurface.normaliseCopy()));
            }

            Vector3 velAlongNormal = VectorUtil.project(velocity, normal);
            c.surface.mu = getFriction(velAlongNormal.magnitude());

            DJoint joint = OdeHelper.createContactJoint(world, contactgroup, contacts.get(i));
            joint.attach(b1, b2);
        }
    }

    public static boolean isInBoundsBall(Vector3 location) {
        return Math.abs(location.x) < SIDE_WALL - BALL_RADIUS && Math.abs(location.y) < BACK_WALL - BALL_RADIUS;
    }

    public static boolean isBehindGoalLine(Vector3 position) {
        return Math.abs(position.y) > BACK_WALL;
    }

    public static BallPath predictBallPath(AgentInput input) {
        try {
            BallSlice key = new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin);
            return pathCache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to compute ball path!", e);
        }
    }

    private void setupWalls() {
        // Floor
        addWallToWorld(new Vector3(0, 0, 1), new Vector3(0, 0, 0));

        // Side walls
        addWallToWorld(new Vector3(1, 0, 0), new Vector3(-SIDE_WALL, 0, 0));
        addWallToWorld(new Vector3(-1, 0, 0), new Vector3(SIDE_WALL, 0, 0));

        // Ceiling
        addWallToWorld(new Vector3(0, 0, 1), new Vector3(0, 0, CEILING + WALL_THICKNESS));


        float sideOffest = (float) (WALL_LENGTH / 2 + Goal.EXTENT);
        float heightOffset = (float) (WALL_LENGTH / 2 + Goal.GOAL_HEIGHT);

        // Wall on the negative side
        addWallToWorld(new Vector3(0, 1, 0), new Vector3(sideOffest, -BACK_WALL, 0));
        addWallToWorld(new Vector3(0, 1, 0), new Vector3(-sideOffest, -BACK_WALL, 0));
        addWallToWorld(new Vector3(0, 1, 0), new Vector3(0, -BACK_WALL, heightOffset));

        // Wall on the positive side
        addWallToWorld(new Vector3(0, -1, 0), new Vector3(sideOffest, BACK_WALL, 0));
        addWallToWorld(new Vector3(0, -1, 0), new Vector3(-sideOffest, BACK_WALL, 0));
        addWallToWorld(new Vector3(0, -1, 0), new Vector3(0, BACK_WALL, heightOffset));


        // 45 angle corners
        addWallToWorld(new Vector3(1, 1, 0), new Vector3((float) -CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, 0));
        addWallToWorld(new Vector3(-1, 1, 0), new Vector3((float) CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, 0));
        addWallToWorld(new Vector3(1, -1, 0), new Vector3((float) -CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, 0));
        addWallToWorld(new Vector3(-1, -1, 0), new Vector3((float) CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, 0));

        // 45 degree angle rails on sides
        addWallToWorld(new Vector3(1, 0, 1), new Vector3(-SIDE_WALL, 0, RAIL_HEIGHT));
        addWallToWorld(new Vector3(-1, 0, 1), new Vector3(SIDE_WALL, 0, RAIL_HEIGHT));

        // 45 degree angle rails on back walls, either side of the goal
        addWallToWorld(new Vector3(0, 1, 1), new Vector3(sideOffest, -BACK_WALL, RAIL_HEIGHT));
        addWallToWorld(new Vector3(0, 1, 1), new Vector3(-sideOffest, -BACK_WALL, RAIL_HEIGHT));
        addWallToWorld(new Vector3(0, -1, 1), new Vector3(sideOffest, BACK_WALL, RAIL_HEIGHT));
        addWallToWorld(new Vector3(0, -1, 1), new Vector3(-sideOffest, BACK_WALL, RAIL_HEIGHT));

        // Floor rails in the corners
        float normalizedVertical = (float) Math.sqrt(2);
        float normalizedFlats = .5f;
        addWallToWorld(
                new Vector3(normalizedFlats, normalizedFlats, normalizedVertical),
                new Vector3((float) -CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));
        addWallToWorld(
                new Vector3(-normalizedFlats, normalizedFlats, normalizedVertical),
                new Vector3((float) CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));
        addWallToWorld(
                new Vector3(normalizedFlats, -normalizedFlats, normalizedVertical),
                new Vector3((float) -CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));
        addWallToWorld(
                new Vector3(-normalizedFlats, -normalizedFlats, normalizedVertical),
                new Vector3((float) CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));

    }

    private void addWallToWorld(Vector3 normal, Vector3 position) {

        DBody body = OdeHelper.createBody(world);
        body.setKinematic();


        DBox box = OdeHelper.createBox(space, WALL_LENGTH, WALL_LENGTH, WALL_THICKNESS);
        box.setBody(body);


        normal = normal.normaliseCopy();
        Vector3 thicknessTweak = normal.scaled(-WALL_THICKNESS / 2);

        Vector3 finalPosition = position.plus(thicknessTweak);

        body.setPosition(finalPosition.x, finalPosition.y, finalPosition.z);

        Vector3 straightUp = new Vector3(0, 0, 1);
        DQuaternionC quat = getRotationFrom(straightUp, normal);
        body.setQuaternion(quat);
    }

    // https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    private DQuaternionC getRotationFrom(Vector3 fromVec, Vector3 toVec) {

        if (fromVec.dotProduct(toVec) > .99999) {
            return new DQuaternion(1, 0, 0, 0);
        }

        Vector3 cross = fromVec.crossProduct(toVec);
        float magnitude = (float) (Math.sqrt(fromVec.magnitudeSquared() * toVec.magnitudeSquared()) + fromVec.dotProduct(toVec));
        DQuaternion rot = new DQuaternion(magnitude, cross.x, cross.y, cross.z);
        rot.normalize();
        return rot;
    }

    private Vector3 getBallPosition() {
        return toV3(ball.getBody().getPosition());
    }

    public BallPath simulateBall(BallSlice start, Duration duration) {
        BallPath ballPath = new BallPath(start);
        simulateBall(ballPath, start.getTime().plus(duration));
        return ballPath;
    }

    private void extendSimulation(BallPath ballPath, LocalDateTime endTime) {
        simulateBall(ballPath, endTime);
    }

    private void simulateBall(BallPath ballPath, LocalDateTime endTime) {
        BallSlice start = ballPath.getEndpoint();

        if (start.getTime().isAfter(endTime)) {
            return;
        }

        ball.getBody().setForce(0, 0, 0);
        ball.getBody().setLinearVel(toV3f(start.getVelocity()));
        ball.getBody().setAngularVel(toV3f(start.spin));
        ball.getBody().setPosition(toV3f(start.getSpace()));

        // Do some simulation
        runSimulation(ballPath, start.getTime(), endTime);
    }

    private void runSimulation(BallPath ballPath, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime simulationTime = LocalDateTime.from(startTime);

        while (simulationTime.isBefore(endTime)) {
            float stepSize = 1.0f / STEPS_PER_SECOND;

            synchronized (lock) {
                space.collide(null, nearCallback);
                world.step(stepSize);
                contactgroup.empty();
            }

            simulationTime = simulationTime.plus(TimeUtil.toDuration(stepSize));
            Vector3 ballVelocity = getBallVelocity();
            Vector3 ballSpin = getBallSpin();
            Vector3 ballPosition = getBallPosition();
            if (Math.abs(ballPosition.y) > BACK_WALL + BALL_RADIUS) {
                // The ball has crossed the goal plane. Freeze it in place.
                // This is handy for making the bot not give up on saves / follow through on shots.
                ball.getBody().setKinematic();
                ball.getBody().setLinearVel(0, 0, 0);
            }
            ballPath.addSlice(new BallSlice(ballPosition, simulationTime, ballVelocity, ballSpin));
        }
    }

    private Vector3 getBallVelocity() {
        return toV3(ball.getBody().getLinearVel());
    }

    private Vector3 getBallSpin() {
        return toV3(ball.getBody().getAngularVel());
    }

    private static DVector3C toV3f(Vector3 v) {
        return new DVector3(v.x, v.y, v.z);
    }

    private static Vector3 toV3(DVector3C v) {
        return new Vector3(v.get0(), v.get1(), v.get2());
    }

    public static boolean isCarNearWall(CarData car) {
        return getDistanceFromWall(car.position) < 2;
    }

    public static double getDistanceFromWall(Vector3 position) {
        double sideWall = SIDE_WALL - Math.abs(position.x);
        double backWall = BACK_WALL - Math.abs(position.y);
        double diagonal = CORNER_ANGLE_CENTER.x + CORNER_ANGLE_CENTER.y - Math.abs(position.x) - Math.abs(position.y);
        return Math.min(Math.min(sideWall, backWall), diagonal);
    }

    public static boolean isCarOnWall(CarData car) {
        return car.hasWheelContact && isCarNearWall(car) && Math.abs(car.orientation.roofVector.z) < 0.05;
    }

    public static boolean isNearFloorEdge(CarData car) {
        return Math.abs(car.position.x) > Goal.EXTENT && getDistanceFromWall(car.position) + car.position.z < 6;
    }
}
