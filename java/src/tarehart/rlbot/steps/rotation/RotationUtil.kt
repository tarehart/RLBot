package tarehart.rlbot.steps.rotation

import tarehart.rlbot.math.vector.Vector3

object RotationUtil {

    fun maxOrbitHeightAbovePlane(axisOfRotation: Vector3, planeNormal: Vector3): Double {
        val xVal = axisOfRotation.projectToPlane(planeNormal).magnitude()
        val angleAbovePlane = Math.acos(xVal)
        return Math.cos(angleAbovePlane)
    }

    fun shortWay(initialRadians: Double): Double {
        var radians = initialRadians

        radians %= (Math.PI * 2)
        if (radians > Math.PI) {
            radians -= Math.PI * 2
        }
        return radians
    }

}
