package tarehart.rlbot.steps.demolition

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.carpredict.CarInterceptPlanner
import tarehart.rlbot.carpredict.CarPredictor
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.Step
import tarehart.rlbot.time.Duration

import java.awt.*
import java.util.Optional

class DemolishEnemyStep : Step {

    private var enemyHadWheelContact: Boolean = false
    private var hasDoubleJumped: Boolean = false
    private var enemyIndex: Int? = null

    override val situation = "Demolishing enemy"

    override fun getOutput(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        val oppositeTeam = input.getTeamRoster(input.team.opposite())
        val enemyCar = enemyIndex?.let { enemyInd -> oppositeTeam.first { it.playerIndex == enemyInd } } ?:
                oppositeTeam.minBy { car.position.distance(it.position) }

        if (enemyCar == null || enemyCar.isDemolished || car.boost == 0.0 && !car.isSupersonic) {
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

            val steering = SteerUtil.steerTowardGroundPosition(car, it.space)

            val secondsTillContact = Duration.between(car.time, it.time).seconds

            val heightAtIntercept = it.space.z
            val needsDoubleJump = heightAtIntercept > 5

            val heightDifference = heightAtIntercept - car.position.z
            if (secondsTillContact < .8 && !enemyCar.hasWheelContact && (enemyHadWheelContact || heightDifference > 1)) {

                if (needsDoubleJump && !hasDoubleJumped && !car.hasWheelContact) {
                    // Let go of A for a moment
                    hasDoubleJumped = true
                } else {
                    steering.withJump()
                }


                if (!car.hasWheelContact) {
                    steering.withSteer(0.0) // Avoid dodging accidentally.
                }
            }

            enemyHadWheelContact = enemyCar.hasWheelContact

            return steering
        }

        return SteerUtil.steerTowardGroundPosition(car, input.boostData, enemyCar.position.flatten())
    }

    override fun canInterrupt(): Boolean {
        return true
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
