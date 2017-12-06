package tarehart.rlbot.ui;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.FullBoost;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
    private Optional<CarData> orangeCar;
    private Optional<CarData> blueCar;
    private CarData myCar;
    private Optional<CarData> enemyCarOptional;
    private Vector3 ball;
    private Vector3 ballPrediction = new Vector3();
    private Optional<Vector3> expectedEnemyContact = Optional.empty();

    public void updateInput(AgentInput input) {
        this.input = input;
        orangeCar = input.getCarData(Bot.Team.ORANGE);
        blueCar = input.getCarData(Bot.Team.BLUE);
        myCar = input.getMyCarData();
        enemyCarOptional = input.getEnemyCarData();
        ball = input.ballPosition;
        realBallColor = input.latestBallTouch.map(bt -> bt.team == Bot.Team.BLUE ? BLUE_BALL_COLOR : ORANGE_BALL_COLOR)
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
        Optional<TacticalSituation> situationOption = TacticsTelemetry.get(myCar.playerIndex);

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

        situationOption.ifPresent(situation -> {
            drawShotDefenseZones(situation, graphics2D);
            drawDefensiveReachZones(situation, graphics2D);
            situation.currentPlan.ifPresent(currentPlan -> drawPlan(currentPlan, graphics2D));
        });

        // Draw the steering waypoint
        // drawWaypoint(graphics2D);

        // Reset the stroke width
        graphics2D.setStroke(new BasicStroke(0));

        // Draw the cars (and their boost values)
        orangeCar.ifPresent(c -> drawCar(c, graphics2D));
        blueCar.ifPresent(c -> drawCar(c, graphics2D));

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
        Optional<Vector2> waypointOption = WaypointTelemetry.get(myCar.team);
        if (waypointOption.isPresent()) {
            graphics2D.setColor(new Color(186, 238, 216));
            graphics2D.setStroke(new BasicStroke(1));
            Vector2 waypoint = waypointOption.get();
            Line2D.Double line = new Line2D.Double(myCar.position.x, myCar.position.y, waypoint.x, waypoint.y);
            graphics2D.draw(line);
        }
    }

    private void drawCar(CarData car, Graphics2D g) {
        // Determine size and rotation of car
        Rectangle2D carShape = new Rectangle2D.Double(-CAR_LENGTH / 2, -CAR_WIDTH / 2, CAR_LENGTH, CAR_WIDTH);
        AffineTransform carTransformation = new AffineTransform();
        carTransformation.translate(car.position.x, car.position.y);
        carTransformation.rotate(car.orientation.noseVector.x, car.orientation.noseVector.y);
        double scale = getHeightScaling(car.position.z);
        carTransformation.scale(scale, scale);
        Shape transformedCar = carTransformation.createTransformedShape(carShape);

        // Draw the car
        g.setColor(car.team == Bot.Team.BLUE ? BLUE_COLOR : ORANGE_COLOR);
        g.fill(transformedCar);
    }

    public static void drawBall(Vector3 position, Graphics2D g, Color color) {
        drawBall(position, g, color, false);
    }

    private static void drawBall(Vector3 position, Graphics2D g, Color color, boolean outline) {

        Ellipse2D.Double ballShape = new Ellipse2D.Double(-BALL_DRAW_RADIUS, -BALL_DRAW_RADIUS, BALL_DRAW_RADIUS * 2, BALL_DRAW_RADIUS * 2);
        AffineTransform ballTransform = new AffineTransform();
        double scale = getHeightScaling(position.z);
        ballTransform.translate(position.x, position.y);
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
        input.fullBoosts.forEach(fullBoost -> {
            if (fullBoost.isActive) {
                drawBoost(fullBoost.location, g);
            }
        });
    }

    private void drawBoost(Vector3 position, Graphics2D g) {
        Ellipse2D.Double ballShape = new Ellipse2D.Double(-BOOST_DRAW_RADIUS, -BOOST_DRAW_RADIUS, BOOST_DRAW_RADIUS * 2, BOOST_DRAW_RADIUS * 2);
        AffineTransform ballTransform = new AffineTransform();
        ballTransform.translate(position.x, position.y);
        Shape transformedBall = ballTransform.createTransformedShape(ballShape);

        g.setColor(BOOST_COLOR);
        g.fill(transformedBall);
    }

    private void drawShotDefenseZones(TacticalSituation situation, Graphics2D g) {
        if(situation.needsDefensiveClear || situation.waitToClear || situation.forceDefensivePosture) {
            g.setColor(new Color(255, 0, 0, 79));
            Vector2 myGoalCenter = GoalUtil.getOwnGoal(myCar.team).getCenter().flatten();
            Polygon shotDefenseZone = ZoneUtil.getShotDefenseZone(ball, myGoalCenter);
            g.draw(shotDefenseZone.getAwtArea());
        }

        if(situation.shotOnGoalAvailable) {
            g.setColor(new Color(0, 255, 0, 79));
            Vector2 enemyGoalCenter = GoalUtil.getEnemyGoal(myCar.team).getCenter().flatten();
            Polygon shotDefenseZone = ZoneUtil.getShotDefenseZone(ball, enemyGoalCenter);
            g.draw(shotDefenseZone.getAwtArea());
        }
    }

    private void drawDefensiveReachZones(TacticalSituation situation, Graphics2D g) {
        Vector3 myGoalCenter = GoalUtil.getOwnGoal(myCar.team).getCenter();
        boolean myCarIsInNet = Math.signum(myCar.position.y) == Math.signum(myGoalCenter.y)
                && Math.abs(myCar.position.y) > Math.abs(myGoalCenter.y);

        if((situation.needsDefensiveClear || situation.waitToClear || situation.forceDefensivePosture) && myCarIsInNet) {
            g.setColor(new Color(0, 255, 0, 79));
            Polygon shotDefenseZone = ZoneUtil.getDefensiveReach(myCar.position, myGoalCenter.flatten());
            g.draw(shotDefenseZone.getAwtArea());
        }

        if(enemyCarOptional.isPresent()) {
            CarData enemyCar = enemyCarOptional.get();
            Vector3 enemyGoalCenter = GoalUtil.getEnemyGoal(myCar.team).getCenter();
            boolean enemyCarIsInNet = Math.signum(enemyCar.position.y) == Math.signum(enemyGoalCenter.y)
                    && Math.abs(enemyCar.position.y) > Math.abs(enemyGoalCenter.y);

            if (situation.shotOnGoalAvailable && enemyCarIsInNet) {
                g.setColor(new Color(255, 0, 0, 79));
                Polygon shotDefenseZone = ZoneUtil.getDefensiveReach(enemyCar.position, enemyGoalCenter.flatten());
                g.draw(shotDefenseZone.getAwtArea());
            }
        }
    }

    private static double getHeightScaling(double height) {
        return 1 + height / 40;
    }
}
