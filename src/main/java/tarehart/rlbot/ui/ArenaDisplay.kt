package tarehart.rlbot.ui

import tarehart.rlbot.AgentInput
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.Polygon
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.routing.PositionFacing
import java.awt.*
import java.awt.geom.*
import java.util.*
import javax.swing.JPanel

class ArenaDisplay : JPanel() {
    private var realBallColor = NEUTRAL_BALL_COLOR

    private var input: AgentInput? = null
    private var myCar: CarData? = null
    private var ball: Vector3? = null
    private var ballPrediction = Vector3()
    private var expectedEnemyContact = Optional.empty<Vector3>()

    fun updateInput(input: AgentInput) {
        this.input = input
        myCar = input.myCarData
        ball = input.ballPosition
        realBallColor = input.latestBallTouch?.let{
            if (it.team === Team.BLUE) BLUE_BALL_COLOR else ORANGE_BALL_COLOR
        } ?: NEUTRAL_BALL_COLOR
    }

    fun updateBallPrediction(ballPrediction: Vector3) {
        this.ballPrediction = ballPrediction
    }

    fun updateExpectedEnemyContact(expectedEnemyContact: Optional<Intercept>) {
        this.expectedEnemyContact = expectedEnemyContact.map { it.space }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val latestInput = input ?: return
        val latestBall = ball ?: return

        // Retrieve situation telemetry
        val situation = myCar?.let { TacticsTelemetry[it.playerIndex] }
        val teamPlan = myCar?.let { TeamTelemetry[it.playerIndex] }

        //Create a Graphics2D object from g
        val graphics2D = g as Graphics2D

        //Antialiasing ON
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON)

        // Flip the y axis and mirror the x axis
        graphics2D.translate(this.width / 2, this.height / 2)
        val scale = this.width / NATURAL_WIDTH
        graphics2D.scale(scale, scale)
        graphics2D.scale(1.0, -1.0)

        // Draw zone definitions
        graphics2D.stroke = BasicStroke(1f)
        graphics2D.color = Color(201, 224, 196)
        Arrays.stream(areas).forEach({ graphics2D.draw(it) })

        // Draw the field outline
        graphics2D.color = Color(59, 133, 81)
        graphics2D.draw(ZoneDefinitions.FULLFIELD.awtArea)

        if (situation != null) {
            drawShotDefenseZones(situation, graphics2D)
            drawDefensiveReachZones(situation, graphics2D)
            Optional.ofNullable(situation.currentPlan).ifPresent { currentPlan -> drawPlan(currentPlan, graphics2D) }
        }

        if(teamPlan != null) {
            drawTeamPlanInfo(teamPlan, graphics2D)
        }

        // Draw the steering waypoint
        // drawWaypoint(graphics2D);

        // Reset the stroke width
        graphics2D.stroke = BasicStroke(0f)

        // Draw the cars (and their boost values)

        val enemyInitiative = situation?.enemyPlayerWithInitiative?.car
        val friendlyInitiative = situation?.teamPlayerWithInitiative?.car

        val blueInitiative = if (latestInput.team === Team.BLUE) friendlyInitiative else enemyInitiative
        val orangeInitiative = if (latestInput.team === Team.ORANGE) friendlyInitiative else enemyInitiative

        latestInput.orangeCars.forEach { c -> drawCar(c, c == this.myCar, orangeInitiative?.let { it == c } == true, graphics2D) }
        latestInput.blueCars.forEach { c -> drawCar(c, c == this.myCar, blueInitiative?.let { it == c } == true, graphics2D) }

        // Draw the ball (and its prediction ghosts)
        drawBall(latestBall, graphics2D, realBallColor)
        drawBall(ballPrediction, graphics2D, Color.BLACK, true)
        expectedEnemyContact.ifPresent { contact -> drawBall(contact, graphics2D, ENEMY_CONTACT_BALL_COLOR) }

        // Draw the available full boost pads
        drawBoosts(graphics2D)
    }

    private fun drawPlan(plan: Plan, graphics2D: Graphics2D) {
        if (!plan.isComplete()) {
            plan.currentStep.drawDebugInfo(graphics2D)
        }
    }

    private fun drawCar(car: CarData, isOurCar: Boolean, hasInitiative: Boolean, g: Graphics2D) {
        // Draw the car
        val c = if (car.team === Team.BLUE) BLUE_COLOR else ORANGE_COLOR
        g.color = if (isOurCar) c.darker() else c
        val positionFacing = PositionFacing(car.position.flatten(), car.orientation.noseVector.flatten())
        drawCar(positionFacing, car.position.z, g)

        if (hasInitiative) {
            val underline = Line2D.Double(-2.0, 0.0, 2.0, 0.0)
            val transform = AffineTransform()
            transform.translate(car.position.x, car.position.y - 4)
            g.stroke = BasicStroke(0.8f)
            g.draw(transform.createTransformedShape(underline))
        }

    }

    private fun drawBoosts(g: Graphics2D) {
        BoostAdvisor.boostData.fullBoosts.forEach {
            if (it.isActive) {
                drawBoost(it.location, g)
            }
        }
    }

    private fun drawBoost(position: Vector3, g: Graphics2D) {
        val ballShape = Ellipse2D.Double(-BOOST_DRAW_RADIUS, -BOOST_DRAW_RADIUS, BOOST_DRAW_RADIUS * 2, BOOST_DRAW_RADIUS * 2)
        val ballTransform = AffineTransform()
        ballTransform.translate(position.x, position.y)
        val transformedBall = ballTransform.createTransformedShape(ballShape)

        g.color = BOOST_COLOR
        g.fill(transformedBall)
    }

    private fun drawShotDefenseZones(situation: TacticalSituation, g: Graphics2D) {
        val car = myCar?: return
        if (situation.needsDefensiveClear || situation.waitToClear || situation.forceDefensivePosture) {
            g.color = Color(255, 0, 0, 79)
            val myGoalCenter = GoalUtil.getOwnGoal(car.team).center.flatten()
            val shotDefenseZone = ZoneUtil.getShotDefenseZone(ball, myGoalCenter)
            g.draw(shotDefenseZone.awtArea)
        }

        if (situation.shotOnGoalAvailable) {
            g.color = Color(0, 255, 0, 79)
            val enemyGoalCenter = GoalUtil.getEnemyGoal(car.team).center.flatten()
            val shotDefenseZone = ZoneUtil.getShotDefenseZone(ball, enemyGoalCenter)
            g.draw(shotDefenseZone.awtArea)
        }
    }

    private fun drawDefensiveReachZones(situation: TacticalSituation, g: Graphics2D) {
        val car = myCar?: return

        val myGoalCenter = GoalUtil.getOwnGoal(car.team).center
        val myCarIsInNet = Math.signum(car.position.y) == Math.signum(myGoalCenter.y) && Math.abs(car.position.y) > Math.abs(myGoalCenter.y)

        if ((situation.needsDefensiveClear || situation.waitToClear || situation.forceDefensivePosture) && myCarIsInNet) {
            g.color = Color(0, 255, 0, 79)
            val shotDefenseZone = ZoneUtil.getDefensiveReach(car.position, myGoalCenter.flatten())
            g.draw(shotDefenseZone.awtArea)
        }

        val enemyWithInitiative = situation.enemyPlayerWithInitiative
        if (enemyWithInitiative != null) {
            val enemyCar = enemyWithInitiative.car
            val enemyGoalCenter = GoalUtil.getEnemyGoal(myCar!!.team).center
            val enemyCarIsInNet = Math.signum(enemyCar.position.y) == Math.signum(enemyGoalCenter.y) && Math.abs(enemyCar.position.y) > Math.abs(enemyGoalCenter.y)

            if (situation.shotOnGoalAvailable && enemyCarIsInNet) {
                g.color = Color(255, 0, 0, 79)
                val shotDefenseZone = ZoneUtil.getDefensiveReach(enemyCar.position, enemyGoalCenter.flatten())
                g.draw(shotDefenseZone.awtArea)
            }
        }
    }

    private fun drawTeamPlanInfo(teamPlan: TeamPlan, g: Graphics2D) {
        val car = myCar?: return

        teamPlan.teamIntents.forEach {
            val line = Line2D.Double(car.position.x, car.position.y, it.car.position.x, it.car.position.y)
            g.stroke = BasicStroke(.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(2f), 0f)
            g.color = TEAM_INTENT_COLOR
            g.draw(line)
        }
    }

    companion object {

        private val areas = arrayOf(clipToField(ZoneDefinitions.BLUE), clipToField(ZoneDefinitions.MID), clipToField(ZoneDefinitions.ORANGE), clipToField(ZoneDefinitions.BOTTOM), clipToField(ZoneDefinitions.TOP), clipToField(ZoneDefinitions.BOTTOMCORNER), clipToField(ZoneDefinitions.TOPCORNER), clipToField(ZoneDefinitions.BLUEBOX), clipToField(ZoneDefinitions.ORANGEBOX))
        private val NEUTRAL_BALL_COLOR = Color(177, 177, 177)
        private val BLUE_BALL_COLOR = Color(77, 147, 177)
        private val ORANGE_BALL_COLOR = Color(226, 159, 63)
        private val ENEMY_CONTACT_BALL_COLOR = Color(140, 24, 194, 19)
        const val NATURAL_WIDTH = 170.0
        private const val CAR_LENGTH = 4
        private const val CAR_WIDTH = 2

        private fun clipToField(p: Polygon): Area {
            val a = p.awtArea
            a.intersect(ZoneDefinitions.FULLFIELD.awtArea)
            return a
        }

        private val BALL_DRAW_RADIUS = 1.9
        private val BOOST_DRAW_RADIUS = 1.3
        val BLUE_COLOR = Color(84, 164, 213)
        val ORANGE_COLOR = Color(247, 151, 66)
        val BOOST_COLOR = Color(255, 207, 64)
        val TEAM_INTENT_COLOR = Color(41, 41, 41, 170)

        fun drawCar(positionFacing: PositionFacing, height: Double, g: Graphics2D) {
            // Determine size and rotation of car
            val carShape = Rectangle2D.Double((-CAR_LENGTH / 2).toDouble(), (-CAR_WIDTH / 2).toDouble(), CAR_LENGTH.toDouble(), CAR_WIDTH.toDouble())
            val carTransformation = AffineTransform()
            carTransformation.translate(positionFacing.position.x, positionFacing.position.y)
            carTransformation.rotate(positionFacing.facing.x, positionFacing.facing.y)
            val scale = getHeightScaling(height)
            carTransformation.scale(scale, scale)
            val transformedCar = carTransformation.createTransformedShape(carShape)
            g.fill(transformedCar)
        }

        fun drawBall(position: Vector3, g: Graphics2D, color: Color) {
            drawBall(position, g, color, false)
        }

        private fun drawBall(position: Vector3, g: Graphics2D, color: Color, outline: Boolean) {

            val ballShape = Ellipse2D.Double(-BALL_DRAW_RADIUS, -BALL_DRAW_RADIUS, BALL_DRAW_RADIUS * 2, BALL_DRAW_RADIUS * 2)
            val ballTransform = AffineTransform()
            val scale = getHeightScaling(position.z)
            ballTransform.translate(position.x, position.y)
            ballTransform.scale(scale, scale)
            val transformedBall = ballTransform.createTransformedShape(ballShape)

            g.color = color
            if (outline) {
                g.stroke = BasicStroke(.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(2f), 0f)
                g.draw(transformedBall)
            } else {
                g.fill(transformedBall)
            }
        }

        private fun getHeightScaling(height: Double): Double {
            return 1 + height / 40
        }
    }
}
