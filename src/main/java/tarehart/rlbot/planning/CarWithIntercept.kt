package tarehart.rlbot.planning

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.physics.DistancePlot

data class CarWithIntercept(val car: CarData, val distancePlot: DistancePlot, val intercept: Intercept?)
