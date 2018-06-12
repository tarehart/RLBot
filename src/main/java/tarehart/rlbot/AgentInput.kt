package tarehart.rlbot

import rlbot.flat.GameInfo
import rlbot.flat.GameTickPacket
import rlbot.flat.PlayerInfo
import rlbot.flat.Touch
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class AgentInput(request: GameTickPacket, val playerIndex: Int, chronometer: Chronometer, spinTracker: SpinTracker, private val frameCount: Long) {

    val blueCars: List<CarData>
    val orangeCars: List<CarData>
    private val ourCar: CarData

    //    private val blueScore: Int
//    private val orangeScore: Int
//    private val blueDemo: Int
//    private val orangeDemo: Int
    val ballPosition: Vector3
    val ballVelocity: Vector3
    val team: Team
    val ballSpin: Vector3
    var time: GameTime
    val matchInfo: MatchInfo
    val latestBallTouch: BallTouch?

    // We can do an unprotected get here because the car corresponding to our own color
    // will always be present because it's us.
    val myCarData: CarData
        get() = ourCar

    init {
        this.matchInfo = getMatchInfo(request.gameInfo())

        val ballPhysics = request.ball().physics()
        val angVel = ballPhysics.angularVelocity()

        // Flip the x-axis, same as all our other vector handling.
        // According to the game, when the spin vector is pointed at you, the ball is spinning clockwise.
        // However, we will invert this concept because the ode4j physics engine disagrees.
        this.ballSpin = Vector3(angVel.x().toDouble(), (-angVel.y()).toDouble(), (-angVel.z()).toDouble())

        ballPosition = convertVector(ballPhysics.location())
        ballVelocity = convertVector(ballPhysics.velocity())

        chronometer.readInput(request.gameInfo().secondsElapsed().toDouble())
        time = chronometer.gameTime
        val elapsedSeconds = chronometer.timeDiff.seconds

        val allCars = ArrayList<CarData>(request.playersLength())
        for (i in 0 until request.playersLength()) {
            allCars.add(convert(request.players(i), i, spinTracker, elapsedSeconds, frameCount))
        }

        val self = allCars[playerIndex]

        this.team = self.team

//        val blueCarInput = if (this.team == Team.BLUE) self else getSomeCar(allCars, Team.BLUE)
//        val orangeCarInput = if (this.team == Team.ORANGE) self else getSomeCar(allCars, Team.ORANGE)
//
//        blueScore = (blueCarInput?.scoreInfo?.goals ?: 0) + (orangeCarInput?.scoreInfo?.ownGoals ?: 0)
//        orangeScore = (orangeCarInput?.scoreInfo?.goals ?: 0) + (blueCarInput?.scoreInfo?.goals ?: 0)
//        blueDemo = blueCarInput?.scoreInfo?.demolitions ?: 0
//        orangeDemo = orangeCarInput?.scoreInfo?.demolitions ?: 0


        blueCars = allCars.filter { it.team == Team.BLUE }
        orangeCars = allCars.filter { it.team == Team.ORANGE }

        val ourTeam = getTeamRoster(this.team)
        ourCar = ourTeam.first { it.playerIndex == playerIndex }

        BoostAdvisor.loadGameTickPacket(request, time)

        this.latestBallTouch = getLatestBallTouch(request.ball().latestTouch(), allCars)
    }

    fun getTeamRoster(team: Team): List<CarData> {
        return if (team == Team.BLUE) blueCars else orangeCars
    }

    //returns every car in the current match
    fun getAllCars(): List<CarData> {
        return blueCars.union(orangeCars).toList()
    }

    //returns every car in the current match except the one provided
    fun getAllOtherCars(indexFilter: Int): List<CarData> {
        return getAllCars().filter { it.playerIndex != indexFilter }
    }

    private fun getLatestBallTouch(touch: Touch?, players: List<CarData>): BallTouch? {

        val latestTouch = touch ?: return null
        if (!latestTouch.playerName().isEmpty()) {
            val toucher = latestTouch.playerName()
            val toucherInfo = players.stream()
                    .filter { pi -> pi.name == toucher }
                    .findFirst()

            if (toucherInfo.isPresent) {
                val realToucher = toucherInfo.get()
                val index = players.indexOf(realToucher)
                val touchTime = GameTime.fromGameSeconds(latestTouch.gameSeconds().toDouble())

                val ballTouch = BallTouch(
                        team = realToucher.team,
                        playerIndex = index,
                        time = touchTime,
                        position = convertVector(latestTouch.location()),
                        normal = convertVector(latestTouch.normal()))

                return ballTouch
            }
        }
        return null
    }

    private fun getSomeCar(playersList: List<CarData>, team: Team): CarData? {
        return playersList.stream().filter { it.team == team }.findFirst().orElse(null)
    }

    private fun getMatchInfo(gameInfo: GameInfo): MatchInfo {
        return MatchInfo(
                matchEnded = gameInfo.isMatchEnded,
                overTime = gameInfo.isOvertime,
                roundActive = gameInfo.isRoundActive,
                timeRemaining = Duration.ofSeconds(gameInfo.gameTimeRemaining().toDouble())
        )
    }

    private fun convert(playerInfo: PlayerInfo, index: Int, spinTracker: SpinTracker, elapsedSeconds: Double, frameCount: Long): CarData {
        val orientation = convert(
                playerInfo.physics().rotation().pitch().toDouble(),
                playerInfo.physics().rotation().yaw().toDouble(),
                playerInfo.physics().rotation().roll().toDouble())

        spinTracker.readInput(orientation, index, elapsedSeconds)

        return CarData(
                position = convertVector(playerInfo.physics().location()),
                velocity = convertVector(playerInfo.physics().velocity()),
                orientation = orientation,
                spin = spinTracker.getSpin(index),
                boost = playerInfo.boost().toDouble(),
                isSupersonic = playerInfo.isSupersonic,
                hasWheelContact = playerInfo.hasWheelContact(),
                team = Companion.teamFromInt(playerInfo.team()),
                playerIndex = index,
                time = time,
                frameCount = frameCount,
                isDemolished = playerInfo.isDemolished,
                name = playerInfo.name()
        )
    }

    /**
     * All params are in radians.
     */
    private fun convert(pitch: Double, yaw: Double, roll: Double): CarOrientation {

        val noseX = -1.0 * Math.cos(pitch) * Math.cos(yaw)
        val noseY = Math.cos(pitch) * Math.sin(yaw)
        val noseZ = Math.sin(pitch)

        val roofX = Math.cos(roll) * Math.sin(pitch) * Math.cos(yaw) + Math.sin(roll) * Math.sin(yaw)
        val roofY = Math.cos(yaw) * Math.sin(roll) - Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw)
        val roofZ = Math.cos(roll) * Math.cos(pitch)

        return CarOrientation(noseVector = Vector3(noseX, noseY, noseZ), roofVector = Vector3(roofX, roofY, roofZ))
    }

    companion object {

        private const val PACKET_DISTANCE_TO_CLASSIC = 50.0

        /**
         * This is strictly for backwards compatibility. It only works in a 1v1 game.
         */
        fun teamToPlayerIndex(team: Team): Int {
            return if (team == Team.BLUE) 0 else 1
        }

        fun teamFromInt(team: Int): Team {
            return if (team == 0) Team.BLUE else Team.ORANGE
        }

        fun convertVector(location: rlbot.flat.Vector3): Vector3 {
            // Invert the X value so that the axes make more sense.
            return Vector3(-location.x() / PACKET_DISTANCE_TO_CLASSIC, location.y() / PACKET_DISTANCE_TO_CLASSIC, location.z() / PACKET_DISTANCE_TO_CLASSIC)
        }
    }
}
