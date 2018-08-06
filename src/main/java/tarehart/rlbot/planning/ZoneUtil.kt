package tarehart.rlbot.planning

import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Polygon
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3

object ZoneUtil {

    // Checks to see if the y position order is: my car, opponent car, ball, my net
    fun isEnemyOffensiveBreakaway(team: Team, myCar: CarData, opponentCar: CarData, ballPosition: Vector3): Boolean {

        if (opponentCar.isDemolished) {
            return false
        }

        return if (team === Team.BLUE) {
            myCar.position.y > opponentCar.position.y && opponentCar.position.y > ballPosition.y
        } else {
            myCar.position.y < opponentCar.position.y && opponentCar.position.y < ballPosition.y
        }
    }

    // Checks to see if the y position order is: opponent car, my car, ball, their net
    fun isMyOffensiveBreakaway(team: Team, myCar: CarData, opponentCar: CarData, ballPosition: Vector3): Boolean {

        if (opponentCar.isDemolished) {
            return true
        }

        return if (team === Team.BLUE) {
            ballPosition.y > myCar.position.y && myCar.position.y > opponentCar.position.y
        } else {
            ballPosition.y < myCar.position.y && myCar.position.y < opponentCar.position.y
        }
    }

    // Checks to see if the given car is the closest to the ball for the given list of cars
    //TODO: Make this check more complex. Look at ball speed, ball direction, car speed, and car direction
    fun carHasPossession(ballPosition: Vector3, car: CarData, otherCars: List<CarData>): Boolean {
        if(otherCars.count() == 0)
            return true

        val ballDistance = ballPosition.distance(car.position)
        var filteredOtherCars = otherCars.filter { it.playerIndex != car.playerIndex }
        filteredOtherCars.forEach {
            val otherBallDistance = ballPosition.distance(it.position)
            if (otherBallDistance < ballDistance)
                return false
        }
        return true
    }

    // Checks to see if the given car is the furthest from the ball for the given list of cars
    fun carIsFurthestFromBall(ballPosition: Vector3, car: CarData, otherCars: List<CarData>): Boolean {
        if(otherCars.count() == 0)
            return false

        val ballDistance = ballPosition.distance(car.position)
        var filteredOtherCars = otherCars.filter { it.playerIndex != car.playerIndex }
        filteredOtherCars.forEach {
            val otherBallDistance = ballPosition.distance(it.position)
            if (otherBallDistance > ballDistance)
                return false
        }
        return true
    }

    fun getShotDefenseZone(ballPosition: Vector3, goalCenter: Vector2): Polygon {
        return Polygon(arrayOf(ballPosition.flatten(), Vector2(goalCenter.x + Goal.EXTENT, goalCenter.y), Vector2(goalCenter.x - Goal.EXTENT, goalCenter.y)))
    }

    fun getDefensiveReach(defender: Vector3, goalCenter: Vector2): Polygon {
        val extendedDistance = 20.0

        val (x, y) = defender.flatten()
        val topPost = Vector2(goalCenter.x + Goal.EXTENT, goalCenter.y)
        val bottomPost = Vector2(goalCenter.x - Goal.EXTENT, goalCenter.y)

        val topSlope = (y - topPost.y) / (x - topPost.x)
        val bottomSlope = (y - bottomPost.y) / (x - bottomPost.x)
        val topFactor = extendedDistance / Math.sqrt(1 + Math.pow(topSlope, 2.0))
        val bottomFactor = extendedDistance / Math.sqrt(1 + Math.pow(bottomSlope, 2.0))

        val extendedTop = Vector2(
                topPost.x + topFactor,
                topPost.y + topFactor * topSlope
        )
        val extendedBottom = Vector2(
                bottomPost.x - bottomFactor,
                bottomPost.y - bottomFactor * bottomSlope
        )
        val topBox = Vector2(extendedTop.x, Math.signum(defender.y) * 70)
        val bottomBox = Vector2(extendedBottom.x, Math.signum(defender.y) * 70)

        return Polygon(arrayOf(defender.flatten(), topPost, extendedTop, topBox, bottomBox, extendedBottom, bottomPost))
    }
}
