package tarehart.rlbot.math

import org.ejml.simple.SimpleMatrix


class Mat3(values: Array<DoubleArray>) {
    val matrix: SimpleMatrix = SimpleMatrix(values)
}
