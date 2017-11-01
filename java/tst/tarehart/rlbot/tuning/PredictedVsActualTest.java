package tarehart.rlbot.tuning;

import com.google.gson.Gson;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import org.junit.Assert;
import org.junit.Test;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class PredictedVsActualTest {


    private static final double THRESHOLD = 4;
    private ArenaModel arenaModel = new ArenaModel();


    private BallPath readRecording(String filename) {
        InputStream in = getClass().getResourceAsStream("/ballrecordings/" + filename);
        Scanner s = new Scanner(in).useDelimiter("\\A");
        String content = s.hasNext() ? s.next() : "";

        Gson gson = new Gson();
        return gson.fromJson(content, BallPath.class);
    }

    @Test
    public void spinlessBackwall() throws UnsupportedEncodingException {
        testFile("spinless-backwall.json");
    }

    @Test
    public void spinlessGround() throws UnsupportedEncodingException {
        testFile("spinless-ground.json");
    }

    private void testFile(String filename) throws UnsupportedEncodingException {

        BallPath actualPath = readRecording(filename);
        actualPath = finesseActualPath(actualPath);
        BallPath predictedPath = makePrediction(actualPath);
        // (-73.29997, 65.447556, 4.5342107) after first time step

        List<BallSlice> actual = actualPath.getSlices();
        for (int i = 0; i < actual.size(); i++) {
            if (i < 20) {
                Vector3 velocity = actual.get(i).getVelocity();
                double speedNow = velocity.magnitude();

                Vector3 velocityAfter = actual.get(i+1).getVelocity();
                double speedNext = velocityAfter.magnitude();

                double drag = speedNext / speedNow;
                double dragPerSpeed = drag / speedNow;
                //System.out.println(String.format("Velocity: %s Speed: %s Drag: %s DragPerSpeed: %s", velocity, speedNow, drag, dragPerSpeed));
            }
        }

        List<BallSlice> predictedSlices = predictedPath.getSlices();

        List<BallSlice> actualTrimmed = new ArrayList<>(predictedSlices.size());

        for (int i = 0; i < predictedSlices.size() - 1; i++) {
            BallSlice actualSlice = actualPath.getMotionAt(predictedSlices.get(i).getTime()).get();
            actualTrimmed.add(actualSlice);
            System.out.println(String.format("A: %s\nP: %s\n", actualSlice, predictedSlices.get(i)));

        }
        actualTrimmed.add(actualPath.getEndpoint());

        for (int i = 0; i < predictedSlices.size(); i++) {

            Vector3 actualSlice = actualTrimmed.get(i).getSpace();
            Vector3 actualToPredicted = predictedSlices.get(i).getSpace().minus(actualSlice);
            double error = new Vector2(actualToPredicted.x, actualToPredicted.y).magnitude();
            if (error > THRESHOLD) {
                Duration duration = Duration.between(actualTrimmed.get(0).getTime(), actualTrimmed.get(i).getTime());
                double seconds = duration.toMillis() / 1000.0;
                Assert.fail(String.format("Diverged to %.2f after %.2f seconds!", error, seconds));
            }
        }
    }

    private BallPath finesseActualPath(BallPath actualPath) {
        Optional<BallSlice> newStart = actualPath.getMotionAt(actualPath.getStartPoint().getTime().plus(Duration.ofMillis(100)));
        BallPath finessed = new BallPath(newStart.get());
        List<BallSlice> slices = actualPath.getSlices();
        for (int i = 0; i < actualPath.getSlices().size(); i++) {
            if (slices.get(i).getTime().isAfter(newStart.get().getTime())) {
                finessed.addSlice(slices.get(i));
            }
        }
        return finessed;
    }

    private BallPath makePrediction(BallPath backWallActual) {
        Duration duration = Duration.between(backWallActual.getStartPoint().getTime(), backWallActual.getEndpoint().getTime());
        return arenaModel.simulateBall(backWallActual.getStartPoint(), duration);
    }

}
