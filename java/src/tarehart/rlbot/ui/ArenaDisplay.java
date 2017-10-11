package tarehart.rlbot.ui;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.planning.ZoneDefinitions;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class ArenaDisplay extends JPanel {

    private static final Polygon[] areas = new Polygon[] {
            ZoneDefinitions.BLUE,
            ZoneDefinitions.MID,
            ZoneDefinitions.ORANGE,
            ZoneDefinitions.BOTTOM,
            ZoneDefinitions.TOP,
            ZoneDefinitions.BOTTOMCORNER,
            ZoneDefinitions.TOPCORNER
    };
    private static final int BALL_DRAW_RADIUS = 5;
    public static final Color BLUE_COLOR = new Color(84, 164, 213);
    public static final Color ORANGE_COLOR = new Color(247, 151, 66);

    private CarData orangeCar;
    private CarData blueCar;
    private Vector2 ball;

    public void updateInput(AgentInput input) {
        orangeCar = input.getCarData(Bot.Team.ORANGE);
        blueCar = input.getCarData(Bot.Team.BLUE);
        ball = input.ballPosition.flatten();
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

        graphics2D.setStroke(new BasicStroke(3));
        graphics2D.setColor(new Color(74, 87, 74, 150));
        Arrays.stream(areas).forEach(a -> paintFieldArea(graphics2D, a));


        graphics2D.setStroke(new BasicStroke(0));

        drawCar(orangeCar, graphics2D);
        drawCar(blueCar, graphics2D);
        drawBall(ball.x, ball.y, graphics2D);

    }

    private void paintFieldArea(Graphics2D g2, Polygon area) {
        g2.draw(area.getAwtArea());
    }

    private void drawCar(CarData car, Graphics2D g) {

        Rectangle2D carShape = new Rectangle2D.Double(-4, -2, 8, 4);
        AffineTransform carRotation = new AffineTransform();
        carRotation.translate(car.position.x, car.position.y);
        carRotation.rotate(car.orientation.noseVector.x, car.orientation.noseVector.y);
        g.setColor(car.team == Bot.Team.BLUE ? BLUE_COLOR : ORANGE_COLOR);
        Shape rotatedCar = carRotation.createTransformedShape(carShape);
        g.fill(rotatedCar);

    }

    private void drawBall(double x, double y, Graphics2D g) {
        g.setColor(new Color(177, 177, 177));
        g.fillOval((int) x - BALL_DRAW_RADIUS, (int) y - BALL_DRAW_RADIUS, BALL_DRAW_RADIUS * 2, BALL_DRAW_RADIUS * 2);
    }
}
