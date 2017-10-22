package tarehart.rlbot.ui;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.WaypointTelemetry;
import tarehart.rlbot.planning.ZoneDefinitions;

import javax.swing.*;
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
    public static final Color REAL_BALL_COLOR = new Color(177, 177, 177);
    private static final Color PREDICTED_BALL_COLOR = new Color(186, 164, 55, 100);
    public static final double NATURAL_WIDTH = 300;
    public static final int CAR_LENGTH = 4;
    public static final int CAR_WIDTH = 2;

    private static Area clipToField(Polygon p) {
        Area a = p.getAwtArea();
        a.intersect(ZoneDefinitions.FULLFIELD.getAwtArea());
        return a;
    }

    private static final double BALL_DRAW_RADIUS = 1.9;
    public static final Color BLUE_COLOR = new Color(84, 164, 213);
    public static final Color ORANGE_COLOR = new Color(247, 151, 66);

    private Optional<CarData> orangeCar;
    private Optional<CarData> blueCar;
    private CarData myCar;
    private Vector3 ball;
    private Vector3 ballPrediction = new Vector3();

    public void updateInput(AgentInput input) {
        orangeCar = input.getCarData(Bot.Team.ORANGE);
        blueCar = input.getCarData(Bot.Team.BLUE);
        myCar = input.getMyCarData();
        ball = input.ballPosition;
    }

    public void updateBallPrediction(Vector3 ballPrediction) {
        this.ballPrediction = ballPrediction;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (ball == null) {
            return; // This helps the UI not spazz out in the editor.
        }

        //Create a Graphics2D object from g
        Graphics2D graphics2D = (Graphics2D)g;

        //Antialiasing ON
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        graphics2D.translate(this.getWidth() / 2, this.getHeight() / 2);
        double scale = this.getWidth() / NATURAL_WIDTH;
        graphics2D.scale(scale, scale);
        graphics2D.scale(-1, 1);
        graphics2D.rotate(Math.PI / 2);

        graphics2D.setStroke(new BasicStroke(1));
        graphics2D.setColor(new Color(201, 224, 196));
        Arrays.stream(areas).forEach(graphics2D::draw);

        graphics2D.setColor(new Color(59, 133, 81));
        graphics2D.draw(ZoneDefinitions.FULLFIELD.getAwtArea());


        drawWaypoint(graphics2D);

        graphics2D.setStroke(new BasicStroke(0));

        orangeCar.ifPresent(c -> drawCar(c, graphics2D));
        blueCar.ifPresent(c -> drawCar(c, graphics2D));

        drawBall(ball, graphics2D, REAL_BALL_COLOR);
        drawBall(ballPrediction, graphics2D, PREDICTED_BALL_COLOR);

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

        Rectangle2D carShape = new Rectangle2D.Double(-CAR_LENGTH / 2, -CAR_WIDTH / 2, CAR_LENGTH, CAR_WIDTH);
        AffineTransform carTransformation = new AffineTransform();
        carTransformation.translate(car.position.x, car.position.y);
        carTransformation.rotate(car.orientation.noseVector.x, car.orientation.noseVector.y);
        double scale = getHeightScaling(car.position.z);
        carTransformation.scale(scale, scale);
        Shape transformedCar = carTransformation.createTransformedShape(carShape);

        g.setColor(car.team == Bot.Team.BLUE ? BLUE_COLOR : ORANGE_COLOR);
        g.fill(transformedCar);

    }

    private void drawBall(Vector3 position, Graphics2D g, Color color) {

        Ellipse2D.Double ballShape = new Ellipse2D.Double(-BALL_DRAW_RADIUS, -BALL_DRAW_RADIUS, BALL_DRAW_RADIUS * 2, BALL_DRAW_RADIUS * 2);
        AffineTransform ballTransform = new AffineTransform();
        double scale = getHeightScaling(position.z);
        ballTransform.translate(position.x, position.y);
        ballTransform.scale(scale, scale);
        Shape transformedBall = ballTransform.createTransformedShape(ballShape);

        g.setColor(color);
        g.fill(transformedBall);
    }

    private double getHeightScaling(double height) {
        return 1 + height / 40;
    }
}
