package tarehart.rlbot.physics;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.tuning.BallTelemetry;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class ArenaModel {

    public static final float SIDE_WALL = 81.92f;
    public static final float BACK_WALL = 102.4f;
    public static final float CEILING = 40.88f;
    public static final float BALL_ANGULAR_DAMPING = 0f;

    private static final int WALL_THICKNESS = 10;
    private static final int WALL_LENGTH = 400;
    public static final float GRAVITY = 13f;
    public static final float BALL_DRAG = .0305f;
    public static final float BALL_RADIUS = 1.8555f;

    public static final Vector2 CORNER_ANGLE_CENTER = new Vector2(70.5, 90.2);

    // The diagonal surfaces that merge the floor and the wall--
    // Higher = more diagonal showing.
    public static final float RAIL_HEIGHT = 2.5f;
    public static final float BALL_RESTITUTION = .583f;
    public static final float WALL_RESTITUTION = 1f;
    public static final float WALL_FRICTION = .5f;
    public static final float BALL_FRICTION = .6f;
    public static final int STEPS_PER_SECOND = 10;
    private static final int STEPS_PER_SECOND_HIGH_RES = 50;

    private DynamicsWorld world;
    private RigidBody ball;

    private static Map<Bot.Team, ArenaModel> modelMap = new HashMap<>();


    public ArenaModel() {
        world = initPhysics();
        setupWalls();
        ball = initBallPhysics();
        world.addRigidBody(ball);
    }

    public static boolean isInBoundsBall(Vector3 location) {
        return Math.abs(location.x) < SIDE_WALL - BALL_RADIUS && Math.abs(location.y) < BACK_WALL - BALL_RADIUS;
    }

    public static boolean isBehindGoalLine(Vector3 position) {
        return Math.abs(position.y) > BACK_WALL;
    }

    public static BallPath predictBallPath(AgentInput input, double seconds) {
        return predictBallPath(input, input.time, TimeUtil.toDuration(seconds));
    }

    public static BallPath predictBallPath(AgentInput input, LocalDateTime startingAt, Duration duration) {

        if (!modelMap.containsKey(input.team)) {
            modelMap.put(input.team, new ArenaModel());
        }

        ArenaModel arenaModel = modelMap.get(input.team);

        Optional<BallPath> pathOption = BallTelemetry.getPath(input.team);

        if (pathOption.isPresent()) {
            BallPath ballPath = pathOption.get();
            if (ballPath.getEndpoint().getTime().isBefore(startingAt.plus(duration))) {
                arenaModel.extendSimulation(ballPath, startingAt.plus(duration));
            }
            return ballPath;
        } else {
            BallPath ballPath = arenaModel.simulateBall(new BallSlice(input.ballPosition, startingAt, input.ballVelocity), duration);
            BallTelemetry.setPath(ballPath, input.team);
            return ballPath;
        }
    }

    private void setupWalls() {
        // Floor
        addWallToWorld(new Vector3f(0, 0, 1), new Vector3f(0, 0, 0));

        // Side walls
        addWallToWorld(new Vector3f(1, 0, 0), new Vector3f(-SIDE_WALL, 0, 0));
        addWallToWorld(new Vector3f(-1, 0, 0), new Vector3f(SIDE_WALL, 0, 0));

        // Ceiling
        addWallToWorld(new Vector3f(0, 0, 1), new Vector3f(0, 0, CEILING + WALL_THICKNESS));


        float sideOffest = (float) (WALL_LENGTH / 2 + Goal.EXTENT);
        float heightOffset = (float) (WALL_LENGTH / 2 + Goal.GOAL_HEIGHT);

        // Wall on the negative side
        addWallToWorld(new Vector3f(0, 1, 0), new Vector3f(sideOffest, -BACK_WALL, 0));
        addWallToWorld(new Vector3f(0, 1, 0), new Vector3f(-sideOffest, -BACK_WALL, 0));
        addWallToWorld(new Vector3f(0, 1, 0), new Vector3f(0, -BACK_WALL, heightOffset));

        // Wall on the positive side
        addWallToWorld(new Vector3f(0, -1, 0), new Vector3f(sideOffest, BACK_WALL, 0));
        addWallToWorld(new Vector3f(0, -1, 0), new Vector3f(-sideOffest, BACK_WALL, 0));
        addWallToWorld(new Vector3f(0, -1, 0), new Vector3f(0, BACK_WALL, heightOffset));


        // 45 angle corners
        addWallToWorld(new Vector3f(1, 1, 0), new Vector3f((float) -CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, 0));
        addWallToWorld(new Vector3f(-1, 1, 0), new Vector3f((float) CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, 0));
        addWallToWorld(new Vector3f(1, -1, 0), new Vector3f((float) -CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, 0));
        addWallToWorld(new Vector3f(-1, -1, 0), new Vector3f((float) CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, 0));

        // 45 degree angle rails on sides
        addWallToWorld(new Vector3f(1, 0, 1), new Vector3f(-SIDE_WALL, 0, RAIL_HEIGHT));
        addWallToWorld(new Vector3f(-1, 0, 1), new Vector3f(SIDE_WALL, 0, RAIL_HEIGHT));

        // 45 degree angle rails on back walls, either side of the goal
        addWallToWorld(new Vector3f(0, 1, 1), new Vector3f(sideOffest, -BACK_WALL, RAIL_HEIGHT));
        addWallToWorld(new Vector3f(0, 1, 1), new Vector3f(-sideOffest, -BACK_WALL, RAIL_HEIGHT));
        addWallToWorld(new Vector3f(0, -1, 1), new Vector3f(sideOffest, BACK_WALL, RAIL_HEIGHT));
        addWallToWorld(new Vector3f(0, -1, 1), new Vector3f(-sideOffest, BACK_WALL, RAIL_HEIGHT));

        // Floor rails in the corners
        float normalizedVertical = (float) Math.sqrt(2);
        float normalizedFlats = .5f;
        addWallToWorld(
                new Vector3f(normalizedFlats, normalizedFlats, normalizedVertical),
                new Vector3f((float) -CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));
        addWallToWorld(
                new Vector3f(-normalizedFlats, normalizedFlats, normalizedVertical),
                new Vector3f((float) CORNER_ANGLE_CENTER.x, (float) -CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));
        addWallToWorld(
                new Vector3f(normalizedFlats, -normalizedFlats, normalizedVertical),
                new Vector3f((float) -CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));
        addWallToWorld(
                new Vector3f(-normalizedFlats, -normalizedFlats, normalizedVertical),
                new Vector3f((float) CORNER_ANGLE_CENTER.x, (float) CORNER_ANGLE_CENTER.y, RAIL_HEIGHT));

    }

    private void addWallToWorld(Vector3f normal, Vector3f position) {

        normal.normalize();

        // A large, flattish box laying on the ground.
        CollisionShape boxGround = new BoxShape(new Vector3f(WALL_LENGTH / 2, WALL_LENGTH / 2, WALL_THICKNESS /2));

        Transform wallTransform = new Transform();
        wallTransform.setIdentity();

        Vector3f thicknessTweak = new Vector3f(normal);
        thicknessTweak.scale(-WALL_THICKNESS / 2);

        Vector3f finalPosition = new Vector3f();
        finalPosition.add(position);
        finalPosition.add(thicknessTweak);
        wallTransform.origin.set(finalPosition);

        Vector3f straightUp = new Vector3f(0, 0, 1);
        Quat4f quat = getRotationFrom(straightUp, normal);
        wallTransform.setRotation(quat);

        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
                0, null, boxGround, new Vector3f());
        RigidBody wall = new RigidBody(rbInfo);
        wall.setRestitution(WALL_RESTITUTION);
        wall.setFriction(WALL_FRICTION);
        wall.setWorldTransform(wallTransform);

        world.addRigidBody(wall);
    }

    // https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
    private Quat4f getRotationFrom(Vector3f fromVec, Vector3f toVec) {

        if (fromVec.dot(toVec) > .99999) {
            return new Quat4f(0, 0, 0, 1);
        }

        Vector3f cross = new Vector3f();
        cross.cross(fromVec, toVec);
        float magnitude = (float) (Math.sqrt(fromVec.lengthSquared() * toVec.lengthSquared()) + fromVec.dot(toVec));
        Quat4f rot = new Quat4f();
        rot.set(cross.x, cross.y, cross.z, magnitude);
        rot.normalize();
        return rot;
    }

    private Vector3 getBallPosition() {
        Transform trans = new Transform();
        ball.getWorldTransform(trans);
        return new Vector3(trans.origin.x, trans.origin.y, trans.origin.z);
    }

    public BallPath simulateBall(BallSlice start, Duration duration) {
        BallPath ballPath = new BallPath(start);
        simulateBall(ballPath, start.getTime().plus(duration));
        return ballPath;
    }

    public BallPath simulateBall(BallSlice start, LocalDateTime endTime) {
        BallPath ballPath = new BallPath(start);
        simulateBall(ballPath, endTime);
        return ballPath;
    }

    public void extendSimulation(BallPath ballPath, LocalDateTime endTime) {
        simulateBall(ballPath, endTime);
    }

    private void simulateBall(BallPath ballPath, LocalDateTime endTime) {
        BallSlice start = ballPath.getEndpoint();
        LocalDateTime simulationTime = LocalDateTime.from(start.getTime());
        if (simulationTime.isAfter(endTime)) {
            return;
        }

        ball.clearForces();
        ball.setLinearVelocity(toV3f(start.getVelocity()));
        ball.setAngularVelocity(toV3f(start.spin));
        Transform ballTransform = new Transform();
        ballTransform.setIdentity();
        ballTransform.origin.set(toV3f(start.getSpace()));
        ball.setWorldTransform(ballTransform);


        // Do some simulation
        while (simulationTime.isBefore(endTime)) {
            float stepsPerSecond = STEPS_PER_SECOND;
            if (simulationTime.isBefore(start.getTime().plusSeconds(1))) {
                stepsPerSecond = STEPS_PER_SECOND_HIGH_RES;
            }

            world.stepSimulation(1.0f / stepsPerSecond, 2, 0.5f / stepsPerSecond);
            simulationTime = simulationTime.plus(TimeUtil.toDuration(1 / stepsPerSecond));
            Vector3 ballVelocity = getBallVelocity();
            Vector3 ballSpin = getBallSpin();
            ballPath.addSlice(new BallSlice(getBallPosition(), simulationTime, ballVelocity, ballSpin));
            double speed = ballVelocity.magnitude();
            if (speed < 10) {
                ball.setFriction(0.1f);
                ball.setDamping(0, BALL_ANGULAR_DAMPING);
            } else {
                ball.setFriction(BALL_FRICTION);
                ball.setDamping(BALL_DRAG, BALL_ANGULAR_DAMPING);
            }
        }
    }

    private Vector3 getBallVelocity() {
        Vector3f ballVel = new Vector3f();
        ball.getLinearVelocity(ballVel);
        return toV3(ballVel);
    }

    private Vector3 getBallSpin() {
        Vector3f ballSpin = new Vector3f();
        ball.getAngularVelocity(ballSpin);
        return toV3(ballSpin);
    }

    private static Vector3f toV3f(Vector3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    private static Vector3 toV3(Vector3f v) {
        return new Vector3(v.x, v.y, v.z);
    }

    private DynamicsWorld initPhysics() {
        // collision configuration contains default setup for memory, collision
        // setup. Advanced users can create their own configuration.
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();

        // use the default collision dispatcher. For parallel processing you
        // can use a diffent dispatcher (see Extras/BulletMultiThreaded)
        CollisionDispatcher dispatcher = new CollisionDispatcher(
                collisionConfiguration);

        // the maximum size of the collision world. Make sure objects stay
        // within these boundaries
        // Don't make the world AABB size too large, it will harm simulation
        // quality and performance
        Vector3f worldAabbMin = new Vector3f(-1000, -1000, -1000);
        Vector3f worldAabbMax = new Vector3f(1000, 1000, 1000);
        int maxProxies = 1024;
        AxisSweep3 overlappingPairCache =
                new AxisSweep3(worldAabbMin, worldAabbMax, maxProxies);

        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        DiscreteDynamicsWorld dynamicsWorld = new DiscreteDynamicsWorld(
                dispatcher, overlappingPairCache, solver,
                collisionConfiguration);

        dynamicsWorld.setGravity(new Vector3f(0, 0, -GRAVITY));

        return dynamicsWorld;
    }

    private RigidBody initBallPhysics() {
        SphereShape collisionShape = new SphereShape(BALL_RADIUS);

        // Create Dynamic Objects
        Transform startTransform = new Transform();
        startTransform.setIdentity();

        float mass = 1f;

        Vector3f localInertia = new Vector3f(0, 0, 0);
        collisionShape.calculateLocalInertia(mass, localInertia);

        startTransform.origin.set(new Vector3f(0, 0, 0));

        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
                mass, null, collisionShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setDamping(BALL_DRAG, BALL_ANGULAR_DAMPING);
        body.setRestitution(BALL_RESTITUTION);
        body.setFriction(BALL_FRICTION);
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);

        return body;
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
        return isCarNearWall(car) && Math.abs(car.orientation.roofVector.z) < 0.05;
    }

    public static boolean isNearFloorEdge(CarData car) {
        return Math.abs(car.position.x) > Goal.EXTENT && getDistanceFromWall(car.position) + car.position.z < 6;
    }
}
