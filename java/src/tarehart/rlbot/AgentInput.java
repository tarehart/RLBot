package tarehart.rlbot;

import tarehart.rlbot.math.vector.Vector3;
import rlbot.input.*;
import tarehart.rlbot.input.*;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgentInput {

    public final Optional<CarData> blueCar;
    public final Optional<CarData> orangeCar;

    public final int blueScore;
    public final int orangeScore;
    public final int blueDemo;
    public final int orangeDemo;
    public final Vector3 ballPosition;
    public final Vector3 ballVelocity;
    public final Bot.Team team;
    public final long frameCount;
    public final Vector3 ballSpin;
    public LocalDateTime time;
    public List<FullBoost> fullBoosts = new ArrayList<>(6);

    public static final int UROT_IN_SEMICIRCLE = 32768;
    public static final double RADIANS_PER_UROT = Math.PI / UROT_IN_SEMICIRCLE;
    private static final double PACKET_DISTANCE_TO_CLASSIC = 50;

    public AgentInput(PyGameTickPacket gameTickPacket, Bot.Team team, Chronometer chronometer, SpinTracker spinTracker, long frameCount) {

        this.frameCount = frameCount;

        PyVector3 angVel = gameTickPacket.gameball.AngularVelocity;
        this.ballSpin = new Vector3(angVel.X, angVel.Y, angVel.Z);

        ballPosition = convert(gameTickPacket.gameball.Location);
        ballVelocity = convert(gameTickPacket.gameball.Velocity);
        boolean isKickoff = ballPosition.flatten().isZero() && ballVelocity.isZero();

        chronometer.readInput(gameTickPacket.gameInfo, isKickoff);

        this.team = team;
        time = chronometer.getGameTime();

        Optional<PyCarInfo> blueCarInput = Optional.empty();
        Optional<PyCarInfo> orangeCarInput = Optional.empty();

        for (PyCarInfo pyCar: gameTickPacket.gamecars) {
            if (pyCar.Team == 0) {
                blueCarInput = Optional.of(pyCar);
            } else {
                orangeCarInput = Optional.of(pyCar);
            }
        }

        blueScore = blueCarInput.map(c -> c.Score.Goals).orElse(0) + orangeCarInput.map(c -> c.Score.OwnGoals).orElse(0);
        orangeScore = orangeCarInput.map(c -> c.Score.Goals).orElse(0) + blueCarInput.map(c -> c.Score.OwnGoals).orElse(0);
        blueDemo = blueCarInput.map(c -> c.Score.Demolitions).orElse(0);
        orangeDemo = orangeCarInput.map(c -> c.Score.Demolitions).orElse(0);

        double elapsedSeconds = TimeUtil.toSeconds(chronometer.getTimeDiff());

        blueCar = blueCarInput.map(c -> convert(c, Bot.Team.BLUE, spinTracker, elapsedSeconds, frameCount));
        orangeCar = orangeCarInput.map(c -> convert(c, Bot.Team.ORANGE, spinTracker, elapsedSeconds, frameCount));

        for (PyBoostInfo boostInfo: gameTickPacket.gameBoosts) {
            Vector3 location = convert(boostInfo.Location);
            Optional<Vector3> confirmedLocation = FullBoost.getFullBoostLocation(location);
            confirmedLocation.ifPresent(loc -> fullBoosts.add(new FullBoost(loc, boostInfo.bActive,
                    boostInfo.bActive ? LocalDateTime.from(time) : time.plus(Duration.ofMillis(boostInfo.Timer)))));
        }
    }

    private CarData convert(PyCarInfo pyCar, Bot.Team team, SpinTracker spinTracker, double elapsedSeconds, long frameCount) {
        Vector3 position = convert(pyCar.Location);
        Vector3 velocity = convert(pyCar.Velocity);
        CarOrientation orientation = convert(pyCar.Rotation);
        double boost = pyCar.Boost;

        spinTracker.readInput(orientation, team, elapsedSeconds);

        final CarSpin spin = spinTracker.getSpin(team);

        return new CarData(position, velocity, orientation, spin, boost,
                pyCar.bSuperSonic, team, time, frameCount);
    }

    private CarOrientation convert(PyRotator rotation) {

        double noseX = -1 * Math.cos(rotation.Pitch * RADIANS_PER_UROT) * Math.cos(rotation.Yaw * RADIANS_PER_UROT);
        double noseY = Math.cos(rotation.Pitch * RADIANS_PER_UROT) * Math.sin(rotation.Yaw * RADIANS_PER_UROT);
        double noseZ = Math.sin(rotation.Pitch * RADIANS_PER_UROT);

        double roofX = Math.cos(rotation.Roll * RADIANS_PER_UROT) * Math.sin(rotation.Pitch * RADIANS_PER_UROT) * Math.cos(rotation.Yaw * RADIANS_PER_UROT) + Math.sin(rotation.Roll * RADIANS_PER_UROT) * Math.sin(rotation.Yaw * RADIANS_PER_UROT);
        double roofY = Math.cos(rotation.Yaw * RADIANS_PER_UROT) * Math.sin(rotation.Roll * RADIANS_PER_UROT) - Math.cos(rotation.Roll * RADIANS_PER_UROT) * Math.sin(rotation.Pitch * RADIANS_PER_UROT) * Math.sin(rotation.Yaw * RADIANS_PER_UROT);
        double roofZ = Math.cos(rotation.Roll * RADIANS_PER_UROT) * Math.cos(rotation.Pitch * RADIANS_PER_UROT);

        return new CarOrientation(new Vector3(noseX, noseY, noseZ), new Vector3(roofX, roofY, roofZ));
    }

    private Vector3 convert(PyVector3 location) {
        // Invert the X value so that the axes make more sense.
        return new Vector3(-location.X / PACKET_DISTANCE_TO_CLASSIC, location.Y / PACKET_DISTANCE_TO_CLASSIC, location.Z / PACKET_DISTANCE_TO_CLASSIC);
    }

    public CarData getMyCarData() {
        // We can do an unprotected get here because the car corresponding to our own color
        // will always be present because it's us.
        return getCarData(team).get();
    }

    public Optional<CarData> getEnemyCarData() {
        return team == Bot.Team.BLUE ? orangeCar : blueCar;
    }

    public Optional<CarData> getCarData(Bot.Team team) {
        return team == Bot.Team.BLUE ? blueCar : orangeCar;
    }
}
