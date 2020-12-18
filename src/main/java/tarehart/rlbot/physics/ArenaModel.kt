package tarehart.rlbot.physics

import rlbot.render.NamedRenderer
import rlbot.render.Renderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.BotHouse
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.*
import tarehart.rlbot.math.BotMath.PI
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.cpp.BallPredictorHelper
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.streams.asStream

class ArenaModel {

    private var previousBallPath: BallPath? = null


    companion object {

        val SIDE_WALL = 81.92f
        val BACK_WALL = 102.4f
        val CEILING = 40.88f
        var GRAVITY = 13F
        const val BALL_RADIUS = 1.8555F

        val CORNER_BEVEL = 11.8 // 45 degree angle walls come in this far from where the rectangular corner would be.
        val CORNER_ANGLE_CENTER = Vector2(SIDE_WALL.toDouble(), BACK_WALL.toDouble()).minus(Vector2(CORNER_BEVEL, CORNER_BEVEL))


        private val arenaPlanes = ArrayList<Plane>()
        val wallIntersectionPoints = ArrayList<Vector2>()

        init {
            setSoccerWalls()
        }

        fun isMicroGravity(): Boolean {
            return abs(GRAVITY) < 5
        }

        fun isLowGravity(): Boolean {
            return BotMath.numberDistance(GRAVITY, 6.5F) < 1
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

        fun isInBounds(location: Vector2): Boolean {
            return isInBounds(location.toVector3(), 0.0)
        }

        // Used for guess and check when setting the wall positions
        fun renderWalls(renderer: Renderer) {
            arenaPlanes.forEach { p ->
                run {
                    RenderUtil.drawSquare(renderer, p, 5F, Color.WHITE)
                    RenderUtil.drawSquare(renderer, p, 10F, Color.WHITE)
                    RenderUtil.drawSquare(renderer, p, 12F, Color.WHITE)
                }
            }
        }

        private fun isInBounds(location: Vector3, buffer: Double): Boolean {
            return getDistanceFromWall(location) > buffer
        }

        fun isBehindGoalLine(position: Vector3): Boolean {
            return kotlin.math.abs(position.y) > BACK_WALL
        }

        private var cachedPath = BallPath(BallSlice(Vector3.ZERO, GameTime.zero(), Vector3.ZERO, Vector3.ZERO))

        private fun isCachedPathValid(start: BallSlice): Boolean {
            val prevPath = cachedPath
            if (prevPath.slices.isEmpty()) {
                return false
            }
            val prevPrediction = prevPath.getMotionAt(start.time)
            if ((prevPath.endpoint.time - start.time) > SIMULATION_DURATION &&
                    prevPrediction != null &&
                    prevPrediction.space.distance(start.space) < .2 &&
                    prevPrediction.velocity.distance(start.velocity) < .2) {

                return true
            }
            return false
        }

        private val pathRenderer = NamedRenderer("reliefBotBallPath")
        private var rainbowCount = 0

        fun predictBallPath(input: AgentInput): BallPath {
            try {
                val slice = BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin)
                synchronized(ArenaModel) {
                    if (!isCachedPathValid(slice)) {
                        cachedPath = BallPredictorHelper.predictPath()
                        if (BotHouse.debugMode) {
                            pathRenderer.startPacket()
                            RenderUtil.drawBallPath(pathRenderer, cachedPath, input.time.plusSeconds(6), RenderUtil.rainbowColor(rainbowCount++), 10)
                            pathRenderer.finishAndSend()
                        }
                    }
                    return cachedPath
                }
            } catch (e: ExecutionException) {
                throw RuntimeException("Failed to compute ball slices!", e)
            }
        }

        fun isCarNearWall(car: CarData): Boolean {
            return getDistanceFromWall(car.position) < 2
        }

        fun getDistanceFromWall(position: Vector3): Float {

            return arenaPlanes.asSequence()
                    .filter { it.normal.z == 0F } // Exclude the floor and ceiling
                    .map { it.distance(position) }.min() ?: 0F
        }

        fun getDistanceFromCeiling(position: Vector3): Float {
            return arenaPlanes.first { it.normal.z == -1F }.distance(position)
        }

        fun isCarOnWall(car: CarData): Boolean {
            return car.hasWheelContact && isCarNearWall(car) && abs(car.orientation.roofVector.z) < 0.05
        }

        fun isNearFloorEdge(position: Vector3): Boolean {
            return abs(position.x) > SoccerGoal.EXTENT && getDistanceFromWall(position) + position.z < 6
        }

        fun getCollisionPlanes(): List<Plane> {
            return arenaPlanes
        }

        fun getWallPlanes(): List<Plane> {
            return getCollisionPlanes().filter { p -> p.normal.z == 0F }
        }

        private fun getWallIntersectionPoints(): List<Vector2> {
            val orderedWalls = getWallPlanes().sortedBy { p -> Atan.atan2(p.position.y.toDouble(), p.position.x.toDouble()) }
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

        fun getNearestPlane(position: Vector3, planes: Collection<Plane> = arenaPlanes): Plane {

            return planes.stream().min { p1, p2 ->
                val p1Distance = p1.distance(position)
                val p2Distance = p2.distance(position)
                if (p1Distance > p2Distance) 1 else -1
            }.get()
        }

        fun clampPosition(position: Vector3, buffer: Double): Vector3 {
            if (isInBounds(position, buffer)) {
                return position
            }
            val nearestPlane = getNearestPlane(position)
            val pointOnPlane = nearestPlane.projectPoint(position)
            return pointOnPlane + nearestPlane.normal * buffer
        }

        fun clampPosition(position: Vector2, buffer: Double): Vector2 {
            if (isInBounds(position.toVector3(), buffer)) {
                return position
            }
            val nearestPlane = getNearestPlane(position.toVector3(), getWallPlanes())
            val pointOnPlane = nearestPlane.projectPoint(position.toVector3())
            return (pointOnPlane + nearestPlane.normal * buffer).flatten()
        }

        fun getBouncePlane(origin: Vector3, direction: Vector3): Plane {
            val longDirection = direction.scaledToMagnitude(500.0)

            val intersectionDistances = arenaPlanes.asSequence().asStream()
                    .collect(Collectors.toMap<Plane, Plane, Float>({p -> p}, { p ->
                        VectorUtil.getPlaneIntersection(p, origin, longDirection)?.distance(origin) ?: Float.MAX_VALUE
                    }))

            return intersectionDistances.entries.stream()
                    .min(Comparator.comparingDouble { ent -> ent.value.toDouble()})
                    .get().key
        }

        fun getBounceNormal(ray: Ray): Ray {
            val longDirection = ray.direction.scaledToMagnitude(500.0)

            val intersectionDistances = arenaPlanes.asSequence()
                    .mapNotNull{ plane -> VectorUtil.getPlaneIntersection(plane, ray.position, longDirection)?.let { Ray(it, plane.normal) } }

            return intersectionDistances.minBy { r -> r.position.distance(ray.position) }!!
        }
    }
}
