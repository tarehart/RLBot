package tarehart.rlbot.steps.strikes

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil

class KickAwayFromOpponent : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3 {
        val toBall = ballPosition.minus(car.position)
        return toBall // This should hopefully never be called.
    }

    override fun getKickDirection(bundle: TacticalBundle, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return getDirection(bundle, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        return true
    }

    override fun isShotOnGoal(): Boolean {
        return false
    }

    private fun getDirection(bundle: TacticalBundle, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        // We will try to make this a useful kick, so let's gather some options from other kick types,
        // and choose one based on the difficulty of the kick and the absence of opponents in that region.
        val car = bundle.agentInput.myCarData
        val toEnemyVectors = bundle.agentInput.getTeamRoster(car.team.opposite()).map { it to (it.position - ballPosition).flatten().normalized() }

        val enemyGoal = GoalUtil.getEnemyGoal(car.team)
        val enemyCorner1 = enemyGoal.center.withX(90)
        val enemyCorner2 = enemyGoal.center.withX(-90) // Avoid thrash

        val kickPreferenceList = listOf(
                KickAtEnemyGoal().getKickDirection(bundle, ballPosition, easyKick),
                WallPass().getKickDirection(bundle, ballPosition, easyKick),
                KickToEnemyHalf().getKickDirection(bundle, ballPosition, easyKick),
                KickAwayFromOwnGoal().getKickDirection(bundle, ballPosition, easyKick),
                enemyCorner1 - ballPosition,
                enemyCorner2 - ballPosition)

        return kickPreferenceList.filterNotNull().maxBy {
            scoreKickOption(toEnemyVectors, it.flatten().normalized(), easyKick.flatten().normalized())
        } ?: easyKick
    }

    /**
     * A high value means this kick is easy and not dangerous.
     */
    private fun scoreKickOption(toEnemyVectors: List<Pair<CarData, Vector2>>, candidateDirection: Vector2, easyKick: Vector2): Float {
        val ease = candidateDirection.dotProduct(easyKick)
        val danger = toEnemyVectors.map {
            candidateDirection.dotProduct(it.second)
        }.max() ?: 0F

        // Crank up the ease a tiny bit so we don't get identical scores causing thrash
        return ease * 1.1F - danger
    }

}
