package tarehart.rlbot.tuning;


import com.google.gson.Gson;
import org.junit.Ignore;
import tarehart.rlbot.math.vector.Vector3;
import org.junit.Assert;
import org.junit.Test;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.physics.BallPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Ignore
public class BallRecorderTest {

    @Test
    public void testFileOutput() throws IOException {

        LocalDateTime now = LocalDateTime.now();
        BallRecorder.startRecording(new BallSlice(new Vector3(0, 0, 0), now, new Vector3(1, 0, 1)), now.plusSeconds(1));
        BallRecorder.recordPosition(new BallSlice(new Vector3(1, 1, 2), now.plusSeconds(1), new Vector3(1, 0, 1)));
        BallRecorder.recordPosition(new BallSlice(new Vector3(2, 2, 5), now.plusSeconds(2), new Vector3(1, 0, 1)));


        Stream<Path> list = Files.list(Paths.get("./" + BallRecorder.DIRECTORY));
        Path path = list.sorted().findFirst().get();
        String json = Files.readAllLines(path).get(0);
        BallPath ballPath = new Gson().fromJson(json, BallPath.class);
        Assert.assertEquals(2, ballPath.getEndpoint().getSpace().z, 0.0001);

    }



}