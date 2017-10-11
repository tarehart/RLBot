package tarehart.rlbot.ui;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.ZoneDefinitions;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class ArenaDisplay extends JPanel {

    private static final Area[] areas = new Area[] {
            clipToField(ZoneDefinitions.BLUE),
            clipToField(ZoneDefinitions.MID),
            clipToField(ZoneDefinitions.ORANGE),
            clipToField(ZoneDefinitions.BOTTOM),
            clipToField(ZoneDefinitions.TOP),
            clipToField(ZoneDefinitions.BOTTOMCORNER),
            clipToField(ZoneDefinitions.TOPCORNER)
    };

    private static Area clipToField(Polygon p) {
        Area a = p.getAwtArea();
        a.intersect(ZoneDefinitions.FULLFIELD.getAwtArea());
        return a;
    }

    private static final int BALL_DRAW_RADIUS = 5;
    public static final Color BLUE_COLOR = new Color(84, 164, 213);
    public static final Color ORANGE_COLOR = new Color(247, 151, 66);

    private CarData orangeCar;
    private CarData blueCar;
    private Vector3 ball;

    public void updateInput(AgentInput input) {
        orangeCar = input.getCarData(Bot.Team.ORANGE);
        blueCar = input.getCarData(Bot.Team.BLUE);
        ball = input.ballPosition;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //Create a Graphics2D object from g
        Graphics2D graphics2D = (Graphics2D)g;

        //Antialiasing ON
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);


        graphics2D.translate(this.getWidth() / 2, this.getHeight() / 2);
        graphics2D.scale(-1, 1);
        graphics2D.rotate(Math.PI / 2);

        graphics2D.setStroke(new BasicStroke(2));
        graphics2D.setColor(new Color(201, 224, 196));
        Arrays.stream(areas).forEach(graphics2D::draw);

        graphics2D.setColor(new Color(59, 133, 81));
        graphics2D.draw(ZoneDefinitions.FULLFIELD.getAwtArea());


        graphics2D.setStroke(new BasicStroke(0));

        drawCar(orangeCar, graphics2D);
        drawCar(blueCar, graphics2D);
        drawBall(ball, graphics2D);

    }

    private void drawCar(CarData car, Graphics2D g) {

        Rectangle2D carShape = new Rectangle2D.Double(-4, -2, 8, 4);
        AffineTransform carTransformation = new AffineTransform();
        carTransformation.translate(car.position.x, car.position.y);
        carTransformation.rotate(car.orientation.noseVector.x, car.orientation.noseVector.y);
        double scale = getHeightScaling(car.position.z);
        carTransformation.scale(scale, scale);
        Shape transformedCar = carTransformation.createTransformedShape(carShape);

        g.setColor(car.team == Bot.Team.BLUE ? BLUE_COLOR : ORANGE_COLOR);
        g.fill(transformedCar);

    }

    private void drawBall(Vector3 position, Graphics2D g) {

        Ellipse2D.Double ballShape = new Ellipse2D.Double(-BALL_DRAW_RADIUS, -BALL_DRAW_RADIUS, BALL_DRAW_RADIUS * 2, BALL_DRAW_RADIUS * 2);
        AffineTransform ballTransform = new AffineTransform();
        double scale = getHeightScaling(position.z);
        ballTransform.translate(position.x, position.y);
        ballTransform.scale(scale, scale);
        Shape transformedBall = ballTransform.createTransformedShape(ballShape);

        g.setColor(new Color(177, 177, 177));
        g.fill(transformedBall);
    }

    private double getHeightScaling(double height) {
        return 1 + height / 40;
    }
}
