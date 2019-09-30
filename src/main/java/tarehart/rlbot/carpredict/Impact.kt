package tarehart.rlbot.carpredict

import tarehart.rlbot.math.Ray
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime


class Impact(position: Vector3, normal: Vector3, val time: GameTime): Ray(position, normal)
