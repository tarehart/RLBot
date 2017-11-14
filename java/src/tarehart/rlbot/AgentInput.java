package tarehart.rlbot;

import rlbot.api.GameData;
import tarehart.rlbot.input.*;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.vector.Vector3;

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
    public final int playerIndex;
    public LocalDateTime time;
    public List<FullBoost> fullBoosts = new ArrayList<>(6);
    public final MatchInfo matchInfo;

    public static final int UROT_IN_SEMICIRCLE = 32768;
    public static final double RADIANS_PER_UROT = Math.PI / UROT_IN_SEMICIRCLE;
    private static final double PACKET_DISTANCE_TO_CLASSIC = 50;

    /**
     * This is strictly for backwards compatibility. It only works in a 1v1 game.
     */
    public static int teamToPlayerIndex(Bot.Team team) {
        return team == Bot.Team.BLUE ? 0 : 1;
    }

    public AgentInput(GameData.GameTickPacket request, int playerIndex, Chronometer chronometer, SpinTracker spinTracker, long frameCount) {
        this.playerIndex = playerIndex;
        this.matchInfo = getMatchInfo(request.getGameInfo());
        this.frameCount = frameCount;

        GameData.Vector3 angVel = request.getBall().getAngularVelocity();

        // Flip the x-axis, same as all our other vector handling.
        // According to the game, when the spin vector is pointed at you, the ball is spinning clockwise.
        // However, we will invert this concept because the ode4j physics engine disagrees.
        this.ballSpin = new Vector3(angVel.getX(), -angVel.getY(), -angVel.getZ());

        ballPosition = convert(request.getBall().getLocation());
        ballVelocity = convert(request.getBall().getVelocity());
        boolean isKickoff = ballPosition.flatten().isZero() && ballVelocity.isZero();

        chronometer.readInput(request.getGameInfo(), isKickoff);

        GameData.PlayerInfo self = request.getPlayers(playerIndex);

        this.team = self.getTeam() == 0 ? Bot.Team.BLUE : Bot.Team.ORANGE;
        time = chronometer.getGameTime();

        Optional<GameData.PlayerInfo> blueCarInput = this.team == Bot.Team.BLUE ? Optional.of(self) : getSomeCar(request.getPlayersList(), Bot.Team.BLUE);
        Optional<GameData.PlayerInfo> orangeCarInput = this.team == Bot.Team.ORANGE ? Optional.of(self) : getSomeCar(request.getPlayersList(), Bot.Team.ORANGE);

        blueScore = blueCarInput.map(c -> c.getScoreInfo().getGoals()).orElse(0) + orangeCarInput.map(c -> c.getScoreInfo().getOwnGoals()).orElse(0);
        orangeScore = orangeCarInput.map(c -> c.getScoreInfo().getGoals()).orElse(0) + blueCarInput.map(c -> c.getScoreInfo().getGoals()).orElse(0);
        blueDemo = blueCarInput.map(c -> c.getScoreInfo().getDemolitions()).orElse(0);
        orangeDemo = orangeCarInput.map(c -> c.getScoreInfo().getDemolitions()).orElse(0);

        double elapsedSeconds = TimeUtil.toSeconds(chronometer.getTimeDiff());

        blueCar = blueCarInput.map(c -> convert(c, Bot.Team.BLUE, spinTracker, elapsedSeconds, frameCount));
        orangeCar = orangeCarInput.map(c -> convert(c, Bot.Team.ORANGE, spinTracker, elapsedSeconds, frameCount));

        for (GameData.BoostInfo boostInfo: request.getBoostPadsList()) {
            Vector3 location = convert(boostInfo.getLocation());
            Optional<Vector3> confirmedLocation = FullBoost.getFullBoostLocation(location);
            confirmedLocation.ifPresent(loc -> fullBoosts.add(new FullBoost(loc, boostInfo.getIsActive(),
                    boostInfo.getIsActive() ? LocalDateTime.from(time) : time.plus(Duration.ofMillis(boostInfo.getTimer())))));
        }
    }

    private Optional<GameData.PlayerInfo> getSomeCar(List<GameData.PlayerInfo> playersList, Bot.Team team) {
        int wantedTeam = teamToPlayerIndex(team);
        return playersList.stream().filter(pi -> pi.getTeam() == wantedTeam).findFirst();
    }

    private MatchInfo getMatchInfo(GameData.GameInfo gameInfo) {
        MatchInfo mi = new MatchInfo();
        mi.matchEnded = gameInfo.getIsMatchEnded();
        mi.overTime = gameInfo.getIsOvertime();
        mi.roundActive = gameInfo.getIsRoundActive();
        mi.timeRemaining = TimeUtil.toDuration(gameInfo.getGameTimeRemaining());
        return mi;
    }

    private CarData convert(GameData.PlayerInfo playerInfo, Bot.Team team, SpinTracker spinTracker, double elapsedSeconds, long frameCount) {
        Vector3 position = convert(playerInfo.getLocation());
        Vector3 velocity = convert(playerInfo.getVelocity());
        CarOrientation orientation = convert(playerInfo.getRotation().getPitch(), playerInfo.getRotation().getYaw(), playerInfo.getRotation().getRoll());
        double boost = playerInfo.getBoost();

        spinTracker.readInput(orientation, team, elapsedSeconds);

        final CarSpin spin = spinTracker.getSpin(team);

        return new CarData(position, velocity, orientation, spin, boost,
                playerInfo.getIsSupersonic(), team, playerIndex, time, frameCount);
    }

    /**
     * All params are in radians.
     */
    private CarOrientation convert(double pitch, double yaw, double roll) {

        double noseX = -1 * Math.cos(pitch) * Math.cos(yaw);
        double noseY = Math.cos(pitch) * Math.sin(yaw);
        double noseZ = Math.sin(pitch);

        double roofX = Math.cos(roll) * Math.sin(pitch) * Math.cos(yaw) + Math.sin(roll) * Math.sin(yaw);
        double roofY = Math.cos(yaw) * Math.sin(roll) - Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw);
        double roofZ = Math.cos(roll) * Math.cos(pitch);

        return new CarOrientation(new Vector3(noseX, noseY, noseZ), new Vector3(roofX, roofY, roofZ));
    }

    private Vector3 convert(GameData.Vector3 location) {
        // Invert the X value so that the axes make more sense.
        return new Vector3(-location.getX() / PACKET_DISTANCE_TO_CLASSIC, location.getY() / PACKET_DISTANCE_TO_CLASSIC, location.getZ() / PACKET_DISTANCE_TO_CLASSIC);
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
