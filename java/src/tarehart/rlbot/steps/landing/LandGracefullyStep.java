package tarehart.rlbot.steps.landing;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;

public class LandGracefullyStep implements Step {
    private static final double SIN_45 = Math.sin(Math.PI / 4);
    public static final Vector3 UP_VECTOR = new Vector3(0, 0, 1);
    public static final double NEEDS_LANDING_HEIGHT = .4;
    private Plan plan = null;
    private Function<AgentInput, Vector2> facingFn;
    public static final Function<AgentInput, Vector2> FACE_BALL = LandGracefullyStep::faceBall;

    private static Vector2 faceBall(AgentInput input) {
        Vector2 toBall = (input.getBallPosition()).minus(input.getMyCarData().getPosition()).flatten();
        return toBall.normalized();
    }

    public LandGracefullyStep() {
        this(input -> input.getMyCarData().getVelocity().flatten());
    }

    public LandGracefullyStep(Function<AgentInput, Vector2> facingFn) {
        this.facingFn = facingFn;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (ArenaModel.isCarOnWall(car) || ArenaModel.isNearFloorEdge(car)) {

            if (WallTouchStep.Companion.hasWallTouchOpportunity(input, ArenaModel.predictBallPath(input))) {
                plan = new Plan().withStep(new WallTouchStep());
                return plan.getOutput(input);
            }

            plan = new Plan().withStep(new DescendFromWallStep());
            return plan.getOutput(input);
        }

        if (car.getPosition().getZ() < NEEDS_LANDING_HEIGHT || ArenaModel.isBehindGoalLine(car.getPosition())) {
            return Optional.empty();
        }

        if (plan == null || plan.isComplete()) {
            plan = planRotation(car, facingFn, input.getTeam());
        }

        return plan.getOutput(input);
    }

    private static Plan planRotation(CarData car, Function<AgentInput, Vector2> facingFn, Bot.Team team) {

        CarOrientation current = car.getOrientation();
        boolean pitchFirst = Math.abs(car.getSpin().getPitchRate()) > 1 || Math.abs(current.getRoofVector().getZ()) > SIN_45;

        return new Plan()
                .withStep(pitchFirst ? new PitchToPlaneStep(UP_VECTOR, true) : new YawToPlaneStep(UP_VECTOR, true))
                .withStep(new RollToPlaneStep(UP_VECTOR))
                .withStep(new YawToPlaneStep(input -> getFacingPlane(facingFn.apply(input))));
    }

    private static Vector3 getFacingPlane(Vector2 desiredFacing) {
        Vector2 rightward = VectorUtil.INSTANCE.rotateVector(desiredFacing, -Math.PI / 2);
        return new Vector3(rightward.getX(), rightward.getY(), 0);
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Landing gracefully " + (plan != null ? "(" + plan.getSituation() + ")" : "");
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
