package tarehart.rlbot.tactics

import tarehart.rlbot.AgentInput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.CarWithIntercept
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

interface TacticsAdvisor {

    fun suitableGameModes(): Set<GameMode>

    fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan?

    fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan

    fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation

    companion object {

        val LOOKAHEAD_SECONDS = 2.0
        val PLAN_HORIZON = Duration.ofSeconds(6.0)

        fun getYAxisWrongSidedness(input: AgentInput): Double {
            val (_, y) = GoalUtil.getOwnGoal(input.team).center
            val playerToBallY = input.ballPosition.y - input.myCarData.position.y
            return playerToBallY * Math.signum(y)
        }

        fun getYAxisWrongSidedness(car: CarData, ball: Vector3): Double {
            val center = GoalUtil.getOwnGoal(car.team).center
            val playerToBallY = ball.y - car.position.y
            return playerToBallY * Math.signum(center.y)
        }

        fun calculateRaceResult(ourContact: GameTime?, enemyContact: GameTime?): Duration {
            if (enemyContact == null) {
                return Duration.ofSeconds(3.0)
            } else if (ourContact == null) {
                return Duration.ofSeconds(-3.0)
            } else {
                return Duration.between(ourContact, enemyContact)
            }
        }

        fun measureEnemyApproachError(input: AgentInput, enemyCar: CarData?, enemyContact: SpaceTime): Double {

            if (enemyCar == null) {
                return 0.0
            }

            val myGoal = GoalUtil.getOwnGoal(input.team)
            val ballToGoal = myGoal.center.minus(enemyContact.space)

            val carToBall = enemyContact.space.minus(enemyCar.position)

            return Vector2.angle(ballToGoal.flatten(), carToBall.flatten())
        }


        fun measureOutOfPosition(input: AgentInput): Double {
            val car = input.myCarData
            val myGoal = GoalUtil.getOwnGoal(input.team)
            val ballToGoal = myGoal.center.minus(input.ballPosition)
            val carToBall = input.ballPosition.minus(car.position)
            val wrongSideVector = VectorUtil.project(carToBall, ballToGoal)
            return wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal))
        }

        fun getCarWithInitiative(cars: List<CarData>, ballPath: BallPath): CarWithIntercept? {

            // TODO: this is pretty expensive. Consider optimizing
            val goGetters = cars.map { CarWithIntercept(it, getSoonestIntercept(it, ballPath)) }
            return goGetters.minBy { it.intercept?.time ?: GameTime(Long.MAX_VALUE) }
        }

        fun getSoonestIntercept(car: CarData, ballPath: BallPath): Intercept? {
            val distancePlot = AccelerationModel.simulateAcceleration(car, PLAN_HORIZON, car.boost)
            return InterceptStep.getSoonestIntercept(car, ballPath, distancePlot, Vector3(), { _, _ -> true })
        }
    }


}
