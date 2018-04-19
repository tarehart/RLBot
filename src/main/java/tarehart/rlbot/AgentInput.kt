package tarehart.rlbot

import rlbot.api.GameData
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.util.Optional

class AgentInput(request: GameData.GameTickPacket, val playerIndex: Int, chronometer: Chronometer, spinTracker: SpinTracker, private val frameCount: Long) {

    val blueCars: List<CarData>
    val orangeCars: List<CarData>
    private val ourCar: CarData

    private val blueScore: Int
    private val orangeScore: Int
    private val blueDemo: Int
    private val orangeDemo: Int
    val ballPosition: Vector3
    val ballVelocity: Vector3
    val team: Team
    val ballSpin: Vector3
    var time: GameTime
    val boostData: BoostData
    val matchInfo: MatchInfo
    val latestBallTouch: Optional<BallTouch>

    // We can do an unprotected get here because the car corresponding to our own color
    // will always be present because it's us.
    val myCarData: CarData
        get() = ourCar

    init {
        this.matchInfo = getMatchInfo(request.gameInfo)

        val angVel = request.ball.angularVelocity

        // Flip the x-axis, same as all our other vector handling.
        // According to the game, when the spin vector is pointed at you, the ball is spinning clockwise.
        // However, we will invert this concept because the ode4j physics engine disagrees.
        this.ballSpin = Vector3(angVel.x.toDouble(), (-angVel.y).toDouble(), (-angVel.z).toDouble())

        ballPosition = convert(request.ball.location)
        ballVelocity = convert(request.ball.velocity)

        chronometer.readInput(request.gameInfo.secondsElapsed.toDouble())

        val self = request.getPlayers(playerIndex)

        this.team = Companion.teamFromInt(self.team)
        time = chronometer.gameTime

        val blueCarInput = if (this.team == Team.BLUE) self else getSomeCar(request.playersList, Team.BLUE)
        val orangeCarInput = if (this.team == Team.ORANGE) self else getSomeCar(request.playersList, Team.ORANGE)

        blueScore = (blueCarInput?.scoreInfo?.goals ?: 0) + (orangeCarInput?.scoreInfo?.ownGoals ?: 0)
        orangeScore = (orangeCarInput?.scoreInfo?.goals ?: 0) + (blueCarInput?.scoreInfo?.goals ?: 0)
        blueDemo = blueCarInput?.scoreInfo?.demolitions ?: 0
        orangeDemo = orangeCarInput?.scoreInfo?.demolitions ?: 0

        val elapsedSeconds = chronometer.timeDiff.seconds

        val allCars = request.playersList.mapIndexed{ index, playerInfo -> convert(playerInfo, index, spinTracker, elapsedSeconds, frameCount)}

        blueCars = allCars.filter{ it.team == Team.BLUE }
        orangeCars = allCars.filter{ it.team == Team.ORANGE }

        val ourTeam = getTeamRoster(this.team)
        ourCar = ourTeam.first{ it.playerIndex == playerIndex }

        boostData = BoostData(
                fullBoosts = request.boostPadsList.mapNotNull {
                    BoostAdvisor.getFullBoostLocation(convert(it.location))?.let {
                        loc -> BoostPad(loc, it.isActive, if (it.isActive) time else time + Duration.ofMillis(it.timer.toLong()), 100.0 ) }
                },
                smallBoosts = request.boostPadsList.mapNotNull {
                    val location = convert(it.location)
                    if (BoostAdvisor.getFullBoostLocation(location) == null) {
                        BoostPad(location, it.isActive, if (it.isActive) time else time + Duration.ofMillis(it.timer.toLong()), 15.0)
                    } else null
                }
        )

        this.latestBallTouch = getLatestBallTouch(request)
    }

    fun getTeamRoster(team: Team): List<CarData> {
        return if (team == Team.BLUE) blueCars else orangeCars
    }

    private fun getLatestBallTouch(request: GameData.GameTickPacket): Optional<BallTouch> {
        val latestTouch = request.ball.latestTouch
        if (!latestTouch.playerName.isEmpty()) {
            val toucher = latestTouch.playerName
            val toucherInfo = request.playersList.stream()
                    .filter { pi -> pi.name == toucher }
                    .findFirst()

            if (toucherInfo.isPresent) {
                val realToucher = toucherInfo.get()
                val index = request.playersList.indexOf(realToucher)
                val touchTime = GameTime.fromGameSeconds(latestTouch.gameSeconds.toDouble())

                val ballTouch = BallTouch(
                        team = Companion.teamFromInt(realToucher.team),
                        playerIndex = index,
                        time = touchTime,
                        position = convert(latestTouch.location),
                        normal = convert(latestTouch.normal))

                return Optional.of(ballTouch)
            }
        }
        return Optional.empty()
    }

    private fun getSomeCar(playersList: List<GameData.PlayerInfo>, team: Team): GameData.PlayerInfo? {
        val wantedTeam = teamToPlayerIndex(team)
        return playersList.stream().filter { it.team == wantedTeam }.findFirst().orElse(null)
    }

    private fun getMatchInfo(gameInfo: GameData.GameInfo): MatchInfo {
        return MatchInfo(
                matchEnded = gameInfo.isMatchEnded,
                overTime = gameInfo.isOvertime,
                roundActive = gameInfo.isRoundActive,
                timeRemaining = Duration.ofSeconds(gameInfo.gameTimeRemaining.toDouble())
        )
    }

    private fun convert(playerInfo: GameData.PlayerInfo, index: Int, spinTracker: SpinTracker, elapsedSeconds: Double, frameCount: Long): CarData {
        val orientation = convert(playerInfo.rotation.pitch.toDouble(), playerInfo.rotation.yaw.toDouble(), playerInfo.rotation.roll.toDouble())

        spinTracker.readInput(orientation, index, elapsedSeconds)

        return CarData(
                position = convert(playerInfo.location),
                velocity = convert(playerInfo.velocity),
                orientation = orientation,
                spin = spinTracker.getSpin(index),
                boost = playerInfo.boost.toDouble(),
                isSupersonic = playerInfo.isSupersonic,
                hasWheelContact = !playerInfo.isMidair,
                team = Companion.teamFromInt(playerInfo.team),
                playerIndex = index,
                time = time,
                frameCount = frameCount,
                isDemolished = playerInfo.isDemolished,
                name = playerInfo.name
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

        return CarOrientation(noseVector = Vector3(noseX, noseY, noseZ), roofVector =  Vector3(roofX, roofY, roofZ))
    }

    private fun convert(location: GameData.Vector3): Vector3 {
        // Invert the X value so that the axes make more sense.
        return Vector3(-location.x / PACKET_DISTANCE_TO_CLASSIC, location.y / PACKET_DISTANCE_TO_CLASSIC, location.z / PACKET_DISTANCE_TO_CLASSIC)
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
    }
}
