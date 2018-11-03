package tarehart.rlbot

import rlbot.flat.GameInfo
import rlbot.flat.GameTickPacket
import rlbot.flat.PlayerInfo
import rlbot.flat.Touch
import rlbot.manager.BotLoopRenderer
import rlbot.render.Renderer
import tarehart.rlbot.bots.BaseBot
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.lang.Math.cos
import java.lang.Math.sin

class AgentInput(
        request: GameTickPacket,
        val playerIndex: Int,
        chronometer: Chronometer,
        frameCount: Long,
        val bot: BaseBot) {

    val allCars: List<CarData>
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

        // Flip the x-axis, same as all our other vector handling.
        // According to the game, when the spin vector is pointed at you, the ball is spinning clockwise.
        // However, we will invert this concept because the ode4j physics engine disagrees.
        this.ballSpin = Vector3.fromRlbot(ballPhysics.angularVelocity())

        ballPosition = Vector3.fromRlbot(ballPhysics.location())
        ballVelocity = Vector3.fromRlbot(ballPhysics.velocity())

        chronometer.readInput(request.gameInfo().secondsElapsed().toDouble())
        time = chronometer.gameTime

        val renderer = BotLoopRenderer.forBotLoop(bot)

        allCars = ArrayList(request.playersLength())
        for (i in 0 until request.playersLength()) {
            allCars.add(convert(request.players(i), i, frameCount, renderer))
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

    //returns every car in the current match except the one provided
    fun getAllOtherCars(indexFilter: Int): List<CarData> {
        return allCars.filter { it.playerIndex != indexFilter }
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
                        position = Vector3.fromRlbot(latestTouch.location()),
                        normal = Vector3.fromRlbot(latestTouch.normal()))

                return ballTouch
            }
        }
        return null
    }

    private fun getMatchInfo(gameInfo: GameInfo): MatchInfo {
        return MatchInfo(
                matchEnded = gameInfo.isMatchEnded,
                overTime = gameInfo.isOvertime,
                roundActive = gameInfo.isRoundActive,
                timeRemaining = Duration.ofSeconds(gameInfo.gameTimeRemaining().toDouble())
        )
    }

    private fun convert(playerInfo: PlayerInfo, index: Int, frameCount: Long, renderer: Renderer): CarData {
        val orientation = convert(
                playerInfo.physics().rotation().pitch().toDouble(),
                playerInfo.physics().rotation().yaw().toDouble(),
                playerInfo.physics().rotation().roll().toDouble())

        val flatAngularVel = playerInfo.physics().angularVelocity()

        // Multiply y and z by -1 to achieve a right handed coordinate system
        val angularVel = Vector3(flatAngularVel.x().toDouble(), -flatAngularVel.y().toDouble(), -flatAngularVel.z().toDouble())

        val spinNew = CarSpin(angularVel, orientation.matrix)

        return CarData(
                position = Vector3.fromRlbot(playerInfo.physics().location()),
                velocity = Vector3.fromRlbot(playerInfo.physics().velocity()),
                orientation = orientation,
                spin = spinNew,
                boost = playerInfo.boost().toDouble(),
                isSupersonic = playerInfo.isSupersonic,
                hasWheelContact = playerInfo.hasWheelContact(),
                team = teamFromInt(playerInfo.team()),
                playerIndex = index,
                time = time,
                frameCount = frameCount,
                isDemolished = playerInfo.isDemolished,
                name = playerInfo.name(),
                renderer = renderer
        )
    }

    private fun convert(pitch: Double, yaw: Double, roll: Double): CarOrientation {

        val CP = cos(pitch)
        val SP = sin(pitch)
        val CY = cos(yaw)
        val SY = sin(yaw)
        val CR = cos(roll)
        val SR = sin(roll)

        val noseX = CP * CY * -1 // Multiply by -1 here to achieve a right handed coordinate system
        val noseY = CP * SY
        val noseZ = SP

        val roofX = (-CR * CY * SP - SR * SY) * -1 // Multiply by -1 here to achieve a right handed coordinate system
        val roofY = -CR * SY * SP + SR * CY
        val roofZ = CP * CR

        return CarOrientation(noseVector = Vector3(noseX, noseY, noseZ), roofVector = Vector3(roofX, roofY, roofZ))
    }

    companion object {

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
