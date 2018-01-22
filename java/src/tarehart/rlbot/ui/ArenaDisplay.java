package tarehart.rlbot.ui;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.routing.PositionFacing;

import javax.swing.*;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.Optional;

public class ArenaDisplay extends JPanel {

    private static final Area[] areas = new Area[] {
            clipToField(ZoneDefinitions.BLUE),
            clipToField(ZoneDefinitions.MID),
            clipToField(ZoneDefinitions.ORANGE),
            clipToField(ZoneDefinitions.BOTTOM),
            clipToField(ZoneDefinitions.TOP),
            clipToField(ZoneDefinitions.BOTTOMCORNER),
            clipToField(ZoneDefinitions.TOPCORNER),
            clipToField(ZoneDefinitions.BLUEBOX),
            clipToField(ZoneDefinitions.ORANGEBOX)
    };
    private static final Color NEUTRAL_BALL_COLOR = new Color(177, 177, 177);
    private static final Color BLUE_BALL_COLOR = new Color(77, 147, 177);
    private static final Color ORANGE_BALL_COLOR = new Color(226, 159, 63);
    private Color realBallColor = NEUTRAL_BALL_COLOR;
    private static final Color ENEMY_CONTACT_BALL_COLOR = new Color(140, 24, 194, 19);
    public static final double NATURAL_WIDTH = 170;
    public static final int CAR_LENGTH = 4;
    public static final int CAR_WIDTH = 2;

    private static Area clipToField(Polygon p) {
        Area a = p.getAwtArea();
        a.intersect(ZoneDefinitions.FULLFIELD.getAwtArea());
        return a;
    }

    private static final double BALL_DRAW_RADIUS = 1.9;
    private static final double BOOST_DRAW_RADIUS = 1.3;
    private static final double BOOST_TEXT_SCALE = 0.5;
    public static final Color BLUE_COLOR = new Color(84, 164, 213);
    public static final Color ORANGE_COLOR = new Color(247, 151, 66);
    public static final Color BOOST_COLOR = new Color(255,207,64);

    private AgentInput input;
    private CarData myCar;
    private Vector3 ball;
    private Vector3 ballPrediction = new Vector3();
    private Optional<Vector3> expectedEnemyContact = Optional.empty();

    public void updateInput(AgentInput input) {
        this.input = input;
        myCar = input.getMyCarData();
        ball = input.getBallPosition();
        realBallColor = input.getLatestBallTouch().map(bt -> bt.getTeam() == Bot.Team.BLUE ? BLUE_BALL_COLOR : ORANGE_BALL_COLOR)
                .orElse(NEUTRAL_BALL_COLOR);
    }

    public void updateBallPrediction(Vector3 ballPrediction) {
        this.ballPrediction = ballPrediction;
    }

    public void updateExpectedEnemyContact(Optional<Intercept> expectedEnemyContact) {
        this.expectedEnemyContact = expectedEnemyContact.map(Intercept::getSpace);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (ball == null) {
            return; // This helps the UI not spazz out in the editor.
        }

        // Retrieve situation telemetry
        TacticalSituation situation = TacticsTelemetry.INSTANCE.get(myCar.getPlayerIndex());

        //Create a Graphics2D object from g
        Graphics2D graphics2D = (Graphics2D)g;

        //Antialiasing ON
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Flip the y axis and mirror the x axis
        graphics2D.translate(this.getWidth() / 2, this.getHeight() / 2);
        double scale = this.getWidth() / NATURAL_WIDTH;
        graphics2D.scale(scale, scale);
        graphics2D.scale(1, -1);

        // Draw zone definitions
        graphics2D.setStroke(new BasicStroke(1));
        graphics2D.setColor(new Color(201, 224, 196));
        Arrays.stream(areas).forEach(graphics2D::draw);

        // Draw the field outline
        graphics2D.setColor(new Color(59, 133, 81));
        graphics2D.draw(ZoneDefinitions.FULLFIELD.getAwtArea());

        if (situation != null) {
            drawShotDefenseZones(situation, graphics2D);
            drawDefensiveReachZones(situation, graphics2D);
            Optional.ofNullable(situation.getCurrentPlan()).ifPresent(currentPlan -> drawPlan(currentPlan, graphics2D));
        }

        // Draw the steering waypoint
        // drawWaypoint(graphics2D);

        // Reset the stroke width
        graphics2D.setStroke(new BasicStroke(0));

        // Draw the cars (and their boost values)

        Optional<CarData> enemyInitiative = Optional.ofNullable(situation).map(TacticalSituation::getEnemyPlayerWithInitiative).map(CarWithIntercept::getCar);
        Optional<CarData> friendlyInitiative = Optional.ofNullable(situation).map(TacticalSituation::getTeamPlayerWithInitiative).map(CarWithIntercept::getCar);

        Optional<CarData> blueInitiative = input.getTeam() == Bot.Team.BLUE ? friendlyInitiative : enemyInitiative;
        Optional<CarData> orangeInitiative = input.getTeam() == Bot.Team.ORANGE ? friendlyInitiative : enemyInitiative;

        input.getOrangeCars().forEach(c -> drawCar(c, c == this.myCar, orangeInitiative.map(or -> or == c).orElse(false), graphics2D));
        input.getBlueCars().forEach(c -> drawCar(c, c == this.myCar, blueInitiative.map(or -> or == c).orElse(false), graphics2D));

        // Draw the ball (and its prediction ghosts)
        drawBall(ball, graphics2D, realBallColor);
        drawBall(ballPrediction, graphics2D, Color.BLACK, true);
        expectedEnemyContact.ifPresent(contact -> drawBall(contact, graphics2D, ENEMY_CONTACT_BALL_COLOR));

        // Draw the available full boost pads
        drawBoosts(graphics2D);
    }

    private void drawPlan(Plan plan, Graphics2D graphics2D) {
        if (!plan.isComplete()) {
            plan.getCurrentStep().drawDebugInfo(graphics2D);
        }
    }

    private void drawWaypoint(Graphics2D graphics2D) {
        Optional<Vector2> waypointOption = WaypointTelemetry.get(myCar.getTeam());
        if (waypointOption.isPresent()) {
            graphics2D.setColor(new Color(186, 238, 216));
            graphics2D.setStroke(new BasicStroke(1));
            Vector2 waypoint = waypointOption.get();
            Line2D.Double line = new Line2D.Double(myCar.getPosition().getX(), myCar.getPosition().getY(), waypoint.getX(), waypoint.getY());
            graphics2D.draw(line);
        }
    }

    private void drawCar(CarData car, boolean isOurCar, boolean hasInitiative, Graphics2D g) {
        // Draw the car
        Color c = car.getTeam() == Bot.Team.BLUE ? BLUE_COLOR : ORANGE_COLOR;
        g.setColor(isOurCar ? c.darker() : c);
        PositionFacing positionFacing = new PositionFacing(car.getPosition().flatten(), car.getOrientation().getNoseVector().flatten());
        drawCar(positionFacing, car.getPosition().getZ(), g);

        if (hasInitiative) {
            Line2D underline = new Line2D.Double(-2, 0, 2, 0);
            AffineTransform transform = new AffineTransform();
            transform.translate(car.getPosition().getX(), car.getPosition().getY() - 4);
            g.setStroke(new BasicStroke(0.8f));
            g.draw(transform.createTransformedShape(underline));
        }

    }

    public static void drawCar(PositionFacing positionFacing, double height, Graphics2D g) {
        // Determine size and rotation of car
        Rectangle2D carShape = new Rectangle2D.Double(-CAR_LENGTH / 2, -CAR_WIDTH / 2, CAR_LENGTH, CAR_WIDTH);
        AffineTransform carTransformation = new AffineTransform();
        carTransformation.translate(positionFacing.getPosition().getX(), positionFacing.getPosition().getY());
        carTransformation.rotate(positionFacing.getFacing().getX(), positionFacing.getFacing().getY());
        double scale = getHeightScaling(height);
        carTransformation.scale(scale, scale);
        Shape transformedCar = carTransformation.createTransformedShape(carShape);
        g.fill(transformedCar);
    }

    public static void drawBall(Vector3 position, Graphics2D g, Color color) {
        drawBall(position, g, color, false);
    }

    private static void drawBall(Vector3 position, Graphics2D g, Color color, boolean outline) {

        Ellipse2D.Double ballShape = new Ellipse2D.Double(-BALL_DRAW_RADIUS, -BALL_DRAW_RADIUS, BALL_DRAW_RADIUS * 2, BALL_DRAW_RADIUS * 2);
        AffineTransform ballTransform = new AffineTransform();
        double scale = getHeightScaling(position.getZ());
        ballTransform.translate(position.getX(), position.getY());
        ballTransform.scale(scale, scale);
        Shape transformedBall = ballTransform.createTransformedShape(ballShape);

        g.setColor(color);
        if (outline) {
            g.setStroke(new BasicStroke(.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0));
            g.draw(transformedBall);
        } else {
            g.fill(transformedBall);
        }
    }

    private void drawBoosts(Graphics2D g) {
        input.getBoostData().getFullBoosts().forEach(fullBoost -> {
            if (fullBoost.isActive()) {
                drawBoost(fullBoost.getLocation(), g);
            }
        });
    }

    private void drawBoost(Vector3 position, Graphics2D g) {
        Ellipse2D.Double ballShape = new Ellipse2D.Double(-BOOST_DRAW_RADIUS, -BOOST_DRAW_RADIUS, BOOST_DRAW_RADIUS * 2, BOOST_DRAW_RADIUS * 2);
        AffineTransform ballTransform = new AffineTransform();
        ballTransform.translate(position.getX(), position.getY());
        Shape transformedBall = ballTransform.createTransformedShape(ballShape);

        g.setColor(BOOST_COLOR);
        g.fill(transformedBall);
    }

    private void drawShotDefenseZones(TacticalSituation situation, Graphics2D g) {
        if(situation.getNeedsDefensiveClear() || situation.getWaitToClear() || situation.getForceDefensivePosture()) {
            g.setColor(new Color(255, 0, 0, 79));
            Vector2 myGoalCenter = GoalUtil.INSTANCE.getOwnGoal(myCar.getTeam()).getCenter().flatten();
            Polygon shotDefenseZone = ZoneUtil.getShotDefenseZone(ball, myGoalCenter);
            g.draw(shotDefenseZone.getAwtArea());
        }

        if(situation.getShotOnGoalAvailable()) {
            g.setColor(new Color(0, 255, 0, 79));
            Vector2 enemyGoalCenter = GoalUtil.INSTANCE.getEnemyGoal(myCar.getTeam()).getCenter().flatten();
            Polygon shotDefenseZone = ZoneUtil.getShotDefenseZone(ball, enemyGoalCenter);
            g.draw(shotDefenseZone.getAwtArea());
        }
    }

    private void drawDefensiveReachZones(TacticalSituation situation, Graphics2D g) {
        Vector3 myGoalCenter = GoalUtil.INSTANCE.getOwnGoal(myCar.getTeam()).getCenter();
        boolean myCarIsInNet = Math.signum(myCar.getPosition().getY()) == Math.signum(myGoalCenter.getY())
                && Math.abs(myCar.getPosition().getY()) > Math.abs(myGoalCenter.getY());

        if((situation.getNeedsDefensiveClear() || situation.getWaitToClear() || situation.getForceDefensivePosture()) && myCarIsInNet) {
            g.setColor(new Color(0, 255, 0, 79));
            Polygon shotDefenseZone = ZoneUtil.getDefensiveReach(myCar.getPosition(), myGoalCenter.flatten());
            g.draw(shotDefenseZone.getAwtArea());
        }

        CarWithIntercept enemyWithInitiative = situation.getEnemyPlayerWithInitiative();
        if(enemyWithInitiative != null) {
            CarData enemyCar = enemyWithInitiative.getCar();
            Vector3 enemyGoalCenter = GoalUtil.INSTANCE.getEnemyGoal(myCar.getTeam()).getCenter();
            boolean enemyCarIsInNet = Math.signum(enemyCar.getPosition().getY()) == Math.signum(enemyGoalCenter.getY())
                    && Math.abs(enemyCar.getPosition().getY()) > Math.abs(enemyGoalCenter.getY());

            if (situation.getShotOnGoalAvailable() && enemyCarIsInNet) {
                g.setColor(new Color(255, 0, 0, 79));
                Polygon shotDefenseZone = ZoneUtil.getDefensiveReach(enemyCar.getPosition(), enemyGoalCenter.flatten());
                g.draw(shotDefenseZone.getAwtArea());
            }
        }
    }

    private static double getHeightScaling(double height) {
        return 1 + height / 40;
    }
}
