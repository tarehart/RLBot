package tarehart.rlbot.physics.cpp

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

class NativeBallPredictorTest {

    @Test
    fun predictPath() {

        val startingSlice = BallSlice(
                Vector3(1.0, 2.0, 3.0),
                GameTime(2000),
                Vector3(1.0, 2.0, 3.0),
                Vector3(1.0, 2.0, 3.0))

        val path = BallPredictorHelper.predictPath(startingSlice, 5f)

        Assert.assertNotNull(path)
        Assert.assertEquals(25, path.slices.size)

    }
}