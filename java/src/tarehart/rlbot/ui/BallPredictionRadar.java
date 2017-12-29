package tarehart.rlbot.ui;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.physics.ArenaModel;

import javax.swing.*;
import java.awt.*;

public class BallPredictionRadar extends JPanel {

    private static final double PREDICTION_SCALE = 8; // Pixels per meter
    private static final int CIRCLE_RADIUS = (int) (ArenaModel.BALL_RADIUS * PREDICTION_SCALE);
    private Vector2 predictionRelative = new Vector2(0, 0);
    private Vector2 velocity = new Vector2(0, 0);

    public BallPredictionRadar() {
        super();
    }

    public void setPredictionCoordinates(Vector2 predictionRelative) {
        this.predictionRelative = predictionRelative;
    }

    public void setVelocity(Vector2 velocity) {
        this.velocity = velocity;
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

        int centerX = this.getWidth() / 2;
        int centerY = this.getWidth() / 2;

        drawCircle(centerX, centerY, new Color(123, 152, 179), graphics2D);

        drawCircle(
                predictionRelative.getX() * PREDICTION_SCALE + centerX,
                predictionRelative.getY() * PREDICTION_SCALE + centerY,
                new Color(167, 224, 178), graphics2D);

        graphics2D.setColor(new Color(250, 150, 129));
        graphics2D.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics2D.drawLine(centerX, centerY, (int) velocity.getX() + centerX, (int) velocity.getY() + centerY);
    }

    private void drawCircle(double x, double y, Color color, Graphics2D g) {
        g.setColor(color);
        g.fillOval((int) x - CIRCLE_RADIUS, (int) y - CIRCLE_RADIUS, CIRCLE_RADIUS * 2,CIRCLE_RADIUS * 2);
    }
}
