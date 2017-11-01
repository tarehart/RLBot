package tarehart.rlbot.physics;


import tarehart.rlbot.math.vector.Vector3;
import org.junit.Assert;
import org.junit.Test;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.planning.Goal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class ArenaModelTest {

    @Test
    public void testConstruct() {
        ArenaModel model = new ArenaModel();
    }


    @Test
    public void testSimulate() {
        ArenaModel model = new ArenaModel();
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, 0, 20), LocalDateTime.now(), new Vector3(5, 60, -10)), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
    }

    @Test
    public void testFallNextToBackWall() {
        ArenaModel model = new ArenaModel();
        float nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, nextToBackWall, 30), LocalDateTime.now(), new Vector3()), Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(nextToBackWall, ballPath.getEndpoint().getSpace().y, .001);
    }

    @Test
    public void testFallToRailNextToBackWall() {
        ArenaModel model = new ArenaModel();
        float nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(Goal.EXTENT + 5, nextToBackWall, 30), LocalDateTime.now(), new Vector3()), Duration.ofSeconds(4));
        System.out.println(nextToBackWall - ballPath.getEndpoint().getSpace().y);
        Assert.assertTrue(nextToBackWall - ballPath.getEndpoint().getSpace().y > 10);
    }

    @Test
    public void testFallToGroundInFrontOfGoal() {
        ArenaModel model = new ArenaModel();
        float nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, nextToBackWall, 30), LocalDateTime.now(), new Vector3()), Duration.ofSeconds(4));
        System.out.println(ballPath.getEndpoint().getSpace());
        Assert.assertEquals(0, ballPath.getEndpoint().space.x, .01);
        Assert.assertEquals(nextToBackWall, ballPath.getEndpoint().space.y, .01);
    }

    @Test
    public void testFallToRailNextToSideWall() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(nextToSideWall, 0, 30), LocalDateTime.now(), new Vector3()), Duration.ofSeconds(4));
        System.out.println(nextToSideWall - ballPath.getEndpoint().getSpace().x);
        Assert.assertTrue(nextToSideWall - ballPath.getEndpoint().getSpace().x > 10);
    }

    @Test
    public void testFallNextToSideWall() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(nextToSideWall, 0, 30), LocalDateTime.now(), new Vector3()), Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(nextToSideWall, ballPath.getEndpoint().getSpace().x, .001);
    }

    @Test
    public void testBounceOffSideWall() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(nextToSideWall - 10, 0, 30), LocalDateTime.now(), new Vector3(20, 0, 0)), Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(0, ballPath.getEndpoint().getSpace().y, .001);
        Assert.assertTrue(ballPath.getEndpoint().getVelocity().x < -10);
        Assert.assertTrue(ballPath.getEndpoint().getVelocity().x > -20);

        Optional<BallSlice> motionAfterBounce = ballPath.getMotionAfterWallBounce(1);
        Assert.assertTrue(motionAfterBounce.isPresent());
        Assert.assertEquals(nextToSideWall, motionAfterBounce.get().getSpace().x, 3);
    }

    @Test
    public void testOpenAirFlight() {
        ArenaModel model = new ArenaModel();
        LocalDateTime now = LocalDateTime.of(2017, 1, 1, 0, 0);
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, 0, 30), now, new Vector3(0, 10, 0)), Duration.ofMillis(200));
        System.out.println(ballPath.getEndpoint());

        double yVal = ballPath.getMotionAt(now.plus(Duration.ofMillis(100))).get().getSpace().y;
        Assert.assertTrue(yVal < 1);
    }

    @Test
    public void testBounceOffSideWallFromCenter() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, 0, 30), LocalDateTime.now(), new Vector3(60, 0, 5)), Duration.ofSeconds(2));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(0, ballPath.getEndpoint().getSpace().y, .6); // This is a bit weird to be honest
        Assert.assertTrue(ballPath.getEndpoint().getVelocity().x < -10);
        Assert.assertTrue(ballPath.getEndpoint().getVelocity().x > -60);

        Optional<BallSlice> motionAfterBounce = ballPath.getMotionAfterWallBounce(1);
        Assert.assertTrue(motionAfterBounce.isPresent());
        System.out.println(nextToSideWall - motionAfterBounce.get().getSpace().x);
        Assert.assertTrue(nextToSideWall - motionAfterBounce.get().getSpace().x < 2.5);
    }

    @Test
    public void testBounceOffCornerAngle() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(nextToSideWall, ArenaModel.BACK_WALL * .7, 30), LocalDateTime.now(), new Vector3(0, 30, 0)), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
        Assert.assertTrue(nextToSideWall - ballPath.getEndpoint().getSpace().x > 10);
    }

    @Test
    public void testBounceIntoPositiveGoal() {
        ArenaModel model = new ArenaModel();
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, ArenaModel.BACK_WALL * .7, 10), LocalDateTime.now(), new Vector3(0, 30, 0)), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
        Assert.assertFalse(ArenaModel.isInBoundsBall(ballPath.getEndpoint().getSpace())); // went into the goal, outside the basic square
    }

    @Test
    public void testRollIntoPositiveGoal() {
        ArenaModel model = new ArenaModel();
        BallPath ballPath = model.simulateBall(new BallSlice(new Vector3(0, ArenaModel.BACK_WALL * .7, ArenaModel.BALL_RADIUS), LocalDateTime.now(), new Vector3(0, 30, 0)), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
        Assert.assertFalse(ArenaModel.isInBoundsBall(ballPath.getEndpoint().getSpace())); // went into the goal, outside the basic square
    }

    @Test
    public void testSpinningFloorBounceX() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(0, 0, 10),
                LocalDateTime.now(),
                new Vector3(),
                new Vector3(10, 0, 0));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
        Assert.assertTrue(ballPath.getEndpoint().velocity.y < -1); // Opposite of game
        Assert.assertTrue(ballPath.getEndpoint().space.y < -1);
    }

    @Test
    public void testSpinningFloorBounceY() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(0, 0, 10),
                LocalDateTime.now(),
                new Vector3(),
                new Vector3(0, 10, 0));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
        Assert.assertTrue(ballPath.getEndpoint().velocity.x > 1); // opposite of game
        Assert.assertTrue(ballPath.getEndpoint().space.x > 1);
    }

    @Test
    public void testSpinningWallBounceX() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(30, ArenaModel.BACK_WALL - 5, 30),
                LocalDateTime.now(),
                new Vector3(0, 10, 0),
                new Vector3(0, 0, 10));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertTrue(ballPath.getEndpoint().velocity.x > 1);
        Assert.assertTrue(ballPath.getEndpoint().space.x > 1);
    }

    @Test
    public void testSpinningWallBounceY() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(ArenaModel.SIDE_WALL - 5, 0, 30),
                LocalDateTime.now(),
                new Vector3(10, 0, 0),
                new Vector3(0, 0, 10));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertTrue(ballPath.getEndpoint().velocity.y < -1);
        Assert.assertTrue(ballPath.getEndpoint().space.y < -1);
    }

    @Test
    public void testWeirdWallBounce() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(19.289791259765625, 77.3377392578125, 7.899403686523438),
                LocalDateTime.now(),
                new Vector3(-7.487739868164063, 53.29123046875, 35.5840673828125),
                new Vector3(0, 0, 0));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
    }



    @Test
    public void testWeirdPartner() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(2.2704116821289064, 90.6794921875, 22.1680859375),
                LocalDateTime.now(),
                new Vector3(2.068851318359375, 46.4755908203125, 13.106075439453125),
                new Vector3(0, 0, 0));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
    }

    @Test
    public void testNormalPartner() {
        ArenaModel model = new ArenaModel();
        BallSlice start = new BallSlice(
                new Vector3(2.3037684631347655, 91.428837890625, 22.3768359375),
                LocalDateTime.now(),
                new Vector3(2.0678350830078127, 46.452763671875, 12.889981689453125),
                new Vector3(0, 0, 0));

        BallPath ballPath = model.simulateBall(start, Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
    }




}