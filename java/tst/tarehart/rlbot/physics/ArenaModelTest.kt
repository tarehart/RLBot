package tarehart.rlbot.physics


import org.junit.Assert
import org.junit.Test
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Goal
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.util.Optional

class ArenaModelTest {

    @Test
    fun testConstruct() {
        val model = ArenaModel()
    }


    @Test
    fun testSimulate() {
        val model = ArenaModel()
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, 0.0, 20.0), GameTime.zero(), Vector3(5.0, 60.0, -10.0)), Duration.ofSeconds(3.0))
        println(ballPath.endpoint)
    }

    @Test
    fun testFallNextToBackWall() {
        val model = ArenaModel()
        val nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, nextToBackWall.toDouble(), 30.0), GameTime.zero(), Vector3()), Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
        Assert.assertEquals(nextToBackWall.toDouble(), ballPath.endpoint.space.y, .001)
    }

    @Test
    fun testFallToRailNextToBackWall() {
        val model = ArenaModel()
        val nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(Goal.EXTENT + 5, nextToBackWall.toDouble(), 30.0), GameTime.zero(), Vector3()), Duration.ofSeconds(4.0))
        println(nextToBackWall - ballPath.endpoint.space.y)
        Assert.assertTrue(nextToBackWall - ballPath.endpoint.space.y > 10)
    }

    @Test
    fun testFallToGroundInFrontOfGoal() {
        val model = ArenaModel()
        val nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, nextToBackWall.toDouble(), 30.0), GameTime.zero(), Vector3()), Duration.ofSeconds(4.0))
        println(ballPath.endpoint.space)
        Assert.assertEquals(0.0, ballPath.endpoint.space.x, .01)
        Assert.assertEquals(nextToBackWall.toDouble(), ballPath.endpoint.space.y, .01)
    }

    @Test
    fun testFallToRailNextToSideWall() {
        val model = ArenaModel()
        val nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(nextToSideWall.toDouble(), 0.0, 30.0), GameTime.zero(), Vector3()), Duration.ofSeconds(4.0))
        println(nextToSideWall - ballPath.endpoint.space.x)
        Assert.assertTrue(nextToSideWall - ballPath.endpoint.space.x > 10)
    }

    @Test
    fun testFallNextToSideWall() {
        val model = ArenaModel()
        val nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(nextToSideWall.toDouble(), 0.0, 30.0), GameTime.zero(), Vector3()), Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
        Assert.assertEquals(nextToSideWall.toDouble(), ballPath.endpoint.space.x, .001)
    }

    @Test
    fun testBounceOffSideWall() {
        val model = ArenaModel()
        val nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3((nextToSideWall - 10).toDouble(), 0.0, 30.0), GameTime.zero(), Vector3(20.0, 0.0, 0.0)), Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
        Assert.assertEquals(0.0, ballPath.endpoint.space.y, .001)
        Assert.assertTrue(ballPath.endpoint.velocity.x < -10)
        Assert.assertTrue(ballPath.endpoint.velocity.x > -20)

        val motionAfterBounce = ballPath.getMotionAfterWallBounce(1)
        Assert.assertNotNull(motionAfterBounce)
        motionAfterBounce ?: throw AssertionError()
        Assert.assertEquals(nextToSideWall.toDouble(), motionAfterBounce.space.x, 3.0)
    }

    @Test
    fun testOpenAirFlight() {
        val model = ArenaModel()
        val now = GameTime.zero()
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, 0.0, 30.0), now, Vector3(0.0, 10.0, 0.0)), Duration.ofMillis(200))
        println(ballPath.endpoint)

        val yVal = ballPath.getMotionAt(now.plus(Duration.ofMillis(100)))?.space?.y
        yVal ?: throw AssertionError()
        Assert.assertTrue(yVal < 1)
    }

    @Test
    fun testBounceOffSideWallFromCenter() {
        val model = ArenaModel()
        val nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, 0.0, 30.0), GameTime.zero(), Vector3(60.0, 0.0, 5.0)), Duration.ofSeconds(2.0))
        println(ballPath.endpoint)
        Assert.assertEquals(0.0, ballPath.endpoint.space.y, .6) // This is a bit weird to be honest
        Assert.assertTrue(ballPath.endpoint.velocity.x < -10)
        Assert.assertTrue(ballPath.endpoint.velocity.x > -60)

        val motionAfterBounce = ballPath.getMotionAfterWallBounce(1)
        motionAfterBounce ?: throw AssertionError()
        println(nextToSideWall - motionAfterBounce.space.x)
        Assert.assertTrue(nextToSideWall - motionAfterBounce.space.x < 2.5)
    }

    @Test
    fun testBounceOffCornerAngle() {
        val model = ArenaModel()
        val nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS
        val ballPath = model.simulateBall(BallSlice(Vector3(nextToSideWall.toDouble(), ArenaModel.BACK_WALL * .7, 30.0), GameTime.zero(), Vector3(0.0, 30.0, 0.0)), Duration.ofSeconds(3.0))
        println(ballPath.endpoint)
        Assert.assertTrue(nextToSideWall - ballPath.endpoint.space.x > 10)
    }

    @Test
    fun testBounceIntoPositiveGoal() {
        val model = ArenaModel()
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, ArenaModel.BACK_WALL * .7, 10.0), GameTime.zero(), Vector3(0.0, 30.0, 0.0)), Duration.ofSeconds(3.0))
        println(ballPath.endpoint)
        Assert.assertFalse(ArenaModel.isInBoundsBall(ballPath.endpoint.space)) // went into the goal, outside the basic square
    }

    @Test
    fun testRollIntoPositiveGoal() {
        val model = ArenaModel()
        val ballPath = model.simulateBall(BallSlice(Vector3(0.0, ArenaModel.BACK_WALL * .7, ArenaModel.BALL_RADIUS.toDouble()), GameTime.zero(), Vector3(0.0, 30.0, 0.0)), Duration.ofSeconds(3.0))
        println(ballPath.endpoint)
        Assert.assertFalse(ArenaModel.isInBoundsBall(ballPath.endpoint.space)) // went into the goal, outside the basic square
    }

    @Test
    fun testSpinningFloorBounceX() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3(0.0, 0.0, 10.0),
                GameTime.zero(),
                Vector3(),
                Vector3(10.0, 0.0, 0.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(3.0))
        println(ballPath.endpoint)
        Assert.assertTrue(ballPath.endpoint.velocity.y < -1) // Opposite of game
        Assert.assertTrue(ballPath.endpoint.space.y < -1)
    }

    @Test
    fun testSpinningFloorBounceY() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3(0.0, 0.0, 10.0),
                GameTime.zero(),
                Vector3(),
                Vector3(0.0, 10.0, 0.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(3.0))
        println(ballPath.endpoint)
        Assert.assertTrue(ballPath.endpoint.velocity.x > 1) // opposite of game
        Assert.assertTrue(ballPath.endpoint.space.x > 1)
    }

    @Test
    fun testSpinningWallBounceX() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3(30.0, (ArenaModel.BACK_WALL - 5).toDouble(), 30.0),
                GameTime.zero(),
                Vector3(0.0, 10.0, 0.0),
                Vector3(0.0, 0.0, 10.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
        Assert.assertTrue(ballPath.endpoint.velocity.x > 1)
        Assert.assertTrue(ballPath.endpoint.space.x > 1)
    }

    @Test
    fun testSpinningWallBounceY() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3((ArenaModel.SIDE_WALL - 5).toDouble(), 0.0, 30.0),
                GameTime.zero(),
                Vector3(10.0, 0.0, 0.0),
                Vector3(0.0, 0.0, 10.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
        Assert.assertTrue(ballPath.endpoint.velocity.y < -1)
        Assert.assertTrue(ballPath.endpoint.space.y < -1)
    }

    @Test
    fun testWeirdWallBounce() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3(19.289791259765625, 77.3377392578125, 7.899403686523438),
                GameTime.zero(),
                Vector3(-7.487739868164063, 53.29123046875, 35.5840673828125),
                Vector3(0.0, 0.0, 0.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
    }


    @Test
    fun testWeirdPartner() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3(2.2704116821289064, 90.6794921875, 22.1680859375),
                GameTime.zero(),
                Vector3(2.068851318359375, 46.4755908203125, 13.106075439453125),
                Vector3(0.0, 0.0, 0.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
    }

    @Test
    fun testNormalPartner() {
        val model = ArenaModel()
        val start = BallSlice(
                Vector3(2.3037684631347655, 91.428837890625, 22.3768359375),
                GameTime.zero(),
                Vector3(2.0678350830078127, 46.452763671875, 12.889981689453125),
                Vector3(0.0, 0.0, 0.0))

        val ballPath = model.simulateBall(start, Duration.ofSeconds(1.0))
        println(ballPath.endpoint)
    }

    @Test
    fun testRollAroundCorner() {
        val model = ArenaModel()
        val nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS - .2
        val ballPath = model.simulateBall(
                BallSlice(
                        Vector3(nextToSideWall.toDouble(), ArenaModel.BACK_WALL * .7, 30.0),
                        GameTime.zero(), Vector3(0.0, 30.0, 0.0),
                        Vector3(0.0, 0.0, -8.0)),
                Duration.ofSeconds(2.0))
        println(ballPath.endpoint)

        Assert.assertTrue(ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS - ballPath.endpoint.space.y < 2)
    }

    @Test
    fun testRollAroundCornerOtherWay() {
        val model = ArenaModel()
        val nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS - .2
        val ballPath = model.simulateBall(
                BallSlice(Vector3(ArenaModel.SIDE_WALL * .6, nextToBackWall, 35.0),
                        GameTime.zero(), Vector3(30.0, 0.0, 0.0),
                        Vector3(0.0, 0.0, 10.0)),
                Duration.ofSeconds(2.0))
        println(ballPath.endpoint)

        Assert.assertTrue(ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS - ballPath.endpoint.space.x < 2)
    }

    @Test
    fun testNearestPlane() {
        val groundPlane = ArenaModel.getNearestPlane(Vector3(0.0, 0.0, 0.0))
        Assert.assertEquals(1.0, groundPlane.normal.z, 0.0)

        val sideWall = ArenaModel.getNearestPlane(Vector3(ArenaModel.SIDE_WALL - 10.0, 30.0, 16.0))
        Assert.assertEquals(-1.0, sideWall.normal.x, 0.0)

        val ceiling = ArenaModel.getNearestPlane(Vector3(16.0, 30.0, ArenaModel.CEILING - 10.0))
        Assert.assertEquals(-1.0, ceiling.normal.z, 0.0)
    }

    @Test
    fun testBouncePlane() {
        val bouncePlane = ArenaModel.getBouncePlane(Vector3(0.0, 0.0, 5.0), Vector3(1.0, 0.0, 0.0))
        Assert.assertEquals(-1.0, bouncePlane.normal.x, 0.0)
    }

}