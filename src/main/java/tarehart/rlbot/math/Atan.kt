package tarehart.rlbot.math

// http://www.java-gaming.org/topics/extremely-fast-atan2/36467/msg/346145/view.html#msg346145

object Atan {

    private val Size_Ac = 100000
    private val Size_Ar = Size_Ac + 1
    private val Pi = Math.PI.toFloat()
    private val Pi_H = Pi / 2

    private val Atan2 = FloatArray(Size_Ar)
    private val Atan2_PM = FloatArray(Size_Ar)
    private val Atan2_MP = FloatArray(Size_Ar)
    private val Atan2_MM = FloatArray(Size_Ar)

    private val Atan2_R = FloatArray(Size_Ar)
    private val Atan2_RPM = FloatArray(Size_Ar)
    private val Atan2_RMP = FloatArray(Size_Ar)
    private val Atan2_RMM = FloatArray(Size_Ar)

    init {
        for (i in 0..Size_Ac) {
            val d = i.toDouble() / Size_Ac
            val x = 1.0
            val y = x * d
            val v = Math.atan2(y, x).toFloat()
            Atan2[i] = v
            Atan2_PM[i] = Pi - v
            Atan2_MP[i] = -v
            Atan2_MM[i] = -Pi + v

            Atan2_R[i] = Pi_H - v
            Atan2_RPM[i] = Pi_H + v
            Atan2_RMP[i] = -Pi_H + v
            Atan2_RMM[i] = -Pi_H - v
        }
    }

    fun atan2(y: Double, x: Double): Double {
        var y = y
        var x = x
        if (y < 0) {
            if (x < 0) {
                //(y < x) because == (-y > -x)
                return if (y < x) {
                    Atan2_RMM[(x / y * Size_Ac).toInt()].toDouble()
                } else {
                    Atan2_MM[(y / x * Size_Ac).toInt()].toDouble()
                }
            } else {
                y = -y
                return if (y > x) {
                    Atan2_RMP[(x / y * Size_Ac).toInt()].toDouble()
                } else {
                    Atan2_MP[(y / x * Size_Ac).toInt()].toDouble()
                }
            }
        } else {
            if (x < 0) {
                x = -x
                return if (y > x) {
                    Atan2_RPM[(x / y * Size_Ac).toInt()].toDouble()
                } else {
                    Atan2_PM[(y / x * Size_Ac).toInt()].toDouble()
                }
            } else {
                return if (y > x) {
                    Atan2_R[(x / y * Size_Ac).toInt()].toDouble()
                } else {
                    Atan2[(y / x * Size_Ac).toInt()].toDouble()
                }
            }
        }
    }
}
