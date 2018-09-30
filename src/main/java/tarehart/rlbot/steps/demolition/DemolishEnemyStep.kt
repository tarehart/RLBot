package tarehart.rlbot.steps.demolition

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.carpredict.CarInterceptPlanner
import tarehart.rlbot.carpredict.CarPredictor
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.rotation.YawToPlaneStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color

class DemolishEnemyStep : NestedPlanStep() {
    private var enemyHadWheelContact: Boolean = false
    private var canDodge: Boolean = true
    private var enemyIndex: Int? = null
    private var momentJumped: GameTime? = null

    private val JUMP_THRESHOLD = 0.8

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        if (car.hasWheelContact && !canDodge) {
            return null // We already attempted a midair dodge, and now we've hit the ground. Give up.
        }
        val oppositeTeam = input.getTeamRoster(input.team.opposite())
        val enemyCar = enemyIndex?.let { enemyInd -> oppositeTeam.first { it.playerIndex == enemyInd } } ?:
                oppositeTeam.filter { !it.isDemolished }.minBy { car.position.distance(it.position) }

        if (enemyCar == null || enemyCar.isDemolished || car.boost < 15 && !car.isSupersonic) {
            return null
        }

        if (enemyCar.position.distance(car.position) < 30) {
            enemyIndex = enemyCar.playerIndex // Commit to demolishing this particular enemy
        }

        val path = CarPredictor.predictCarMotion(enemyCar, Duration.ofSeconds(4.0))

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost)

        val carIntercept = CarInterceptPlanner.getCarIntercept(car, path, distancePlot)

        carIntercept?.let {

            // TODO: deal with cases where car is driving toward arena boundary

            //        if (ArenaModel.isInBoundsBall(slices.getSlices().get(slices.getSlices().size() - 1).space)) {
            //            BotLog.println("Whoops", car.playerIndex);
            //        }

            var steering = SteerUtil.steerTowardGroundPosition(car, it.space)

            val secondsTillContact = Duration.between(car.time, it.time).seconds

            val heightAtIntercept = it.space.z
            val needsDoubleJump = heightAtIntercept > 5

            val toIntercept = it.space - car.position

            val renderer = BotLoopRenderer.forBotLoop(input.bot)
            RenderUtil.drawSphere(renderer, it.space, 1.0, Color.RED)



            val heightDifference = toIntercept.z

            if (secondsTillContact > JUMP_THRESHOLD) {
                val beneathImpact = it.space.flatten().toVector3()
                renderer.drawLine3d(Color.ORANGE, it.space.toRlbot(), beneathImpact.toRlbot())
                val towardJump = beneathImpact + toIntercept.scaledToMagnitude(-car.velocity.magnitude() * JUMP_THRESHOLD)
                renderer.drawLine3d(Color.ORANGE, beneathImpact.toRlbot(), towardJump.toRlbot())

            } else if (!enemyCar.hasWheelContact) {

                steering = AgentOutput().withBoost()

                if (needsDoubleJump && canDodge && !car.hasWheelContact && momentJumped != null) {

                    val timeSinceJump = Duration.between(momentJumped!!, car.time)
                    if (timeSinceJump < Duration.ofMillis(200)) {
                        return AgentOutput().withJump()
                    }

                    canDodge = false
                    return startPlan(Plan().unstoppable()
                            .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withBoost()))
                            .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withBoost().withJump())), input)
                } else {

                    val steerCorrection = car.velocity.flatten().correctionAngle(toIntercept.flatten())
                    if (Math.abs(steerCorrection) > Math.PI / 20) {
                        return startPlan(Plan().unstoppable()
                                .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withBoost()))
                                .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()
                                        .withBoost()
                                        .withRoll(-Math.signum(steerCorrection))
                                        .withJump())), input)
                    }

//                    if (!canDodge) {
//                        val rightVector = VectorUtil.rotateVector(toIntercept.flatten(), -Math.PI / 2).toVector3()
//                        val yawAmount = YawToPlaneStep({ rightVector }).getOutput(input)?.yaw ?: 0.0
//                        steering.withYaw(yawAmount * 0.1)
//                    }


//
//                    val myPositionAtImpact = CarPredictor.predictCarMotion(car, it.time - car.time).lastSlice.space
//                    steering.withJump(myPositionAtImpact.z < it.space.z)
                    if (momentJumped == null) {
                        momentJumped = car.time
                    }
                    steering.withJump()
                }
            }

            enemyHadWheelContact = enemyCar.hasWheelContact

            return steering
        }

        return SteerUtil.steerTowardGroundPositionGreedily(car, enemyCar.position.flatten())
    }

    override fun getLocalSituation(): String {
        return "Demolishing enemy"
    }
}
