package tarehart.rlbot.physics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import rlbot.render.Renderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.cpp.BallPredictorHelper
import tarehart.rlbot.planning.Goal
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import java.awt.Color
import java.lang.Math.*
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.streams.asStream

class ArenaModel {

    private var previousBallPath: BallPath? = null
    fun simulateBall(start: BallSlice): BallPath {
        val prevPath = previousBallPath
        val ballPath: BallPath
        if (prevPath != null) {
            val prevPrediction = prevPath.getMotionAt(start.time)
            if ((prevPath.endpoint.time - start.time) > SIMULATION_DURATION &&
                    prevPrediction != null &&
                    prevPrediction.space.distance(start.space) < .3 &&
                    prevPrediction.space.flatten().distance(start.space.flatten()) < .1 &&
                    prevPrediction.velocity.distance(start.velocity) < .3 &&
                    prevPrediction.velocity.flatten().distance(start.velocity.flatten()) < .1) {

                ballPath = prevPath // Previous prediction is still legit, build on top of it.
            } else {
                ballPath = BallPredictorHelper.predictPath()
            }
        } else {
            ballPath = BallPredictorHelper.predictPath()
        }
        previousBallPath = ballPath
        return ballPath
    }

    companion object {

        val SIDE_WALL = 81.92f
        val BACK_WALL = 102.4f
        val CEILING = 40.88f
        val GRAVITY = 13f
        val BALL_RADIUS = 1.8555f

        val CORNER_BEVEL = 11.8 // 45 degree angle walls come in this far from where the rectangular corner would be.
        val CORNER_ANGLE_CENTER = Vector2(SIDE_WALL.toDouble(), BACK_WALL.toDouble()).minus(Vector2(CORNER_BEVEL, CORNER_BEVEL))


        private val arenaPlanes = ArrayList<Plane>()
        val wallIntersectionPoints = ArrayList<Vector2>()

        init {
            setSoccerWalls()
        }

        fun setSoccerWalls() {

            arenaPlanes.clear()

            // Floor
            arenaPlanes.add(Plane(Vector3(0.0, 0.0, 1.0), Vector3(0.0, 0.0, 0.0)))

            // Side walls
            arenaPlanes.add(Plane(Vector3(1.0, 0.0, 0.0), Vector3((-SIDE_WALL).toDouble(), 0.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(-1.0, 0.0, 0.0), Vector3(SIDE_WALL.toDouble(), 0.0, 0.0)))

            // Ceiling
            arenaPlanes.add(Plane(Vector3(0.0, 0.0, -1.0), Vector3(0.0, 0.0, CEILING.toDouble())))

            // 45 angle corners
            arenaPlanes.add(Plane(Vector3(1.0, 1.0, 0.0), Vector3(-CORNER_ANGLE_CENTER.x, -CORNER_ANGLE_CENTER.y, 0.0)))
            arenaPlanes.add(Plane(Vector3(-1.0, 1.0, 0.0), Vector3(CORNER_ANGLE_CENTER.x, -CORNER_ANGLE_CENTER.y, 0.0)))
            arenaPlanes.add(Plane(Vector3(1.0, -1.0, 0.0), Vector3(-CORNER_ANGLE_CENTER.x, CORNER_ANGLE_CENTER.y, 0.0)))
            arenaPlanes.add(Plane(Vector3(-1.0, -1.0, 0.0), Vector3(CORNER_ANGLE_CENTER.x, CORNER_ANGLE_CENTER.y, 0.0)))


            // Back walls
            arenaPlanes.add(Plane(Vector3(0.0, 1.0, 0.0), Vector3(0.0, (-BACK_WALL).toDouble(), 0.0)))
            arenaPlanes.add(Plane(Vector3(0.0, -1.0, 0.0), Vector3(0.0, BACK_WALL.toDouble(), 0.0)))

            updateWallIntersectionPoints()
        }

        fun setHoopsWalls() {

            arenaPlanes.clear()

            // Floor
            arenaPlanes.add(Plane(Vector3(0.0, 0.0, 1.0), Vector3(0.0, 0.0, 0.0)))

            // Side walls
            arenaPlanes.add(Plane(Vector3(1.0, 0.0, 0.0), Vector3(-59.0, 0.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(-1.0, 0.0, 0.0), Vector3(59.0, 0.0, 0.0)))

            // Ceiling
            arenaPlanes.add(Plane(Vector3(0.0, 0.0, -1.0), Vector3(0.0, 0.0, 36.0)))

            // 45 angle corners
            arenaPlanes.add(Plane(Vector3(1.0, 1.0, 0.0), Vector3(-53.0, -62.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(-1.0, 1.0, 0.0), Vector3(53.0, -62.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(1.0, -1.0, 0.0), Vector3(-53.0, 62.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(-1.0, -1.0, 0.0), Vector3(53.0, 62.0, 0.0)))


            // Back walls
            arenaPlanes.add(Plane(Vector3(0.0, 1.0, 0.0), Vector3(0.0, -72.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(0.0, -1.0, 0.0), Vector3(0.0, 72.0, 0.0)))

            updateWallIntersectionPoints()
        }

        fun setDropshotWalls() {

            arenaPlanes.clear()

            // Floor
            arenaPlanes.add(Plane(Vector3(0.0, 0.0, 1.0), Vector3(0.0, 0.0, 0.0)))

            // Ceiling
            arenaPlanes.add(Plane(Vector3(0.0, 0.0, -1.0), Vector3(0.0, 0.0, 40.0)))

            // Hexagonal side walls
            val hexagonAngle = PI / 6
            arenaPlanes.add(Plane(Vector3(cos(hexagonAngle), sin(hexagonAngle), 0.0), Vector3(-80.0, -45.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(cos(-hexagonAngle), sin(-hexagonAngle), 0.0), Vector3(-80.0, 45.0, 0.0)))

            arenaPlanes.add(Plane(Vector3(-cos(hexagonAngle), -sin(hexagonAngle), 0.0), Vector3(80.0, 45.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(-cos(-hexagonAngle), -sin(-hexagonAngle), 0.0), Vector3(80.0, -45.0, 0.0)))


            // Back walls
            arenaPlanes.add(Plane(Vector3(0.0, 1.0, 0.0), Vector3(0.0, -92.0, 0.0)))
            arenaPlanes.add(Plane(Vector3(0.0, -1.0, 0.0), Vector3(0.0, 92.0, 0.0)))

            updateWallIntersectionPoints()
        }

        val SIMULATION_DURATION = Duration.ofSeconds(5.0)

        private val mainModel = ArenaModel()

        private val pathCache = CacheBuilder.newBuilder()
                .maximumSize(10)
                .build(object : CacheLoader<BallSlice, BallPath>() {
                    @Throws(Exception::class)
                    override fun load(key: BallSlice): BallPath {
                        synchronized(lock) {
                            // Always use a new ArenaModel. There's a nasty bug
                            // where bounces stop working properly and I can't track it down.
                            return mainModel.simulateBall(key)
                        }
                    }
                })

        private val lock = Any()

        fun isInBounds(location: Vector2): Boolean {
            return isInBounds(location.toVector3(), 0.0)
        }

        fun isInBoundsBall(location: Vector3): Boolean {
            return isInBounds(location, BALL_RADIUS.toDouble())
        }

        fun renderWalls(renderer: Renderer) {
            arenaPlanes.forEach { p ->
                run {
                    RenderUtil.drawSquare(renderer, p, 5.0, Color.WHITE)
                    RenderUtil.drawSquare(renderer, p, 10.0, Color.WHITE)
                    RenderUtil.drawSquare(renderer, p, 12.0, Color.WHITE)
                }
            }
        }

        private fun isInBounds(location: Vector3, buffer: Double): Boolean {
            return getDistanceFromWall(location) > buffer
        }

        fun isBehindGoalLine(position: Vector3): Boolean {
            return Math.abs(position.y) > BACK_WALL
        }

        fun predictBallPath(input: AgentInput): BallPath {
            try {
                val key = BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin)
                return pathCache.get(key)
            } catch (e: ExecutionException) {
                throw RuntimeException("Failed to compute ball slices!", e)
            }

        }

        fun isCarNearWall(car: CarData): Boolean {
            return getDistanceFromWall(car.position) < 2
        }

        fun getDistanceFromWall(position: Vector3): Double {

            return arenaPlanes.asSequence()
                    .filter { it.normal.z == 0.0 } // Exclude the floor and ceiling
                    .map { it.distance(position) }.min() ?: 0.0
        }

        fun isCarOnWall(car: CarData): Boolean {
            return car.hasWheelContact && isCarNearWall(car) && Math.abs(car.orientation.roofVector.z) < 0.05
        }

        fun isNearFloorEdge(position: Vector3): Boolean {
            return Math.abs(position.x) > Goal.EXTENT && getDistanceFromWall(position) + position.z < 6
        }

        fun getCollisionPlanes(): List<Plane> {
            return arenaPlanes
        }

        fun getWallPlanes(): List<Plane> {
            return getCollisionPlanes().filter { p -> p.normal.z == 0.0 }
        }

        private fun getWallIntersectionPoints(): List<Vector2> {
            val orderedWalls = getWallPlanes().sortedBy { p -> Math.atan2(p.position.y, p.position.x) }
            val points = ArrayList<Vector2>(orderedWalls.size)

            for (i in 0 until orderedWalls.size - 1) {
                val intersection = orderedWalls[i].intersect(orderedWalls[i + 1])
                intersection?.let { points.add(it.position.flatten()) }
            }

            val lastIntersection = orderedWalls.first().intersect(orderedWalls.last())
            lastIntersection?.let { points.add(it.position.flatten()) }
            return points
        }

        private fun updateWallIntersectionPoints() {
            wallIntersectionPoints.clear()
            wallIntersectionPoints.addAll(getWallIntersectionPoints())
        }

        fun getNearestPlane(position: Vector3): Plane {

            return arenaPlanes.stream().min { p1, p2 ->
                val p1Distance = p1.distance(position)
                val p2Distance = p2.distance(position)
                if (p1Distance > p2Distance) 1 else -1
            }.get()
        }

        fun getBouncePlane(origin: Vector3, direction: Vector3): Plane {
            val longDirection = direction.scaledToMagnitude(500.0)

            val intersectionDistances = arenaPlanes.asSequence().asStream()
                    .collect(Collectors.toMap<Plane, Plane, Double>({p -> p}, { p ->
                        VectorUtil.getPlaneIntersection(p, origin, longDirection)?.distance(origin) ?: Double.MAX_VALUE
                    }))

            return intersectionDistances.entries.stream().min(Comparator.comparingDouble{ ent -> ent.value}).get().key
        }
    }
}
