package tarehart.rlbot.steps.landing;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;

import java.time.Duration;
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
        Vector2 toBall = (input.ballPosition).minus(input.getMyCarData().position).flatten();
        return toBall.normalized();
    }


    public LandGracefullyStep() {
        this(input -> input.getMyCarData().velocity.flatten());
    }

    public LandGracefullyStep(Function<AgentInput, Vector2> facingFn) {
        this.facingFn = facingFn;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (ArenaModel.isCarOnWall(car) || ArenaModel.isNearFloorEdge(car)) {

            if (WallTouchStep.hasWallTouchOpportunity(input, ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(4)))) {
                plan = new Plan().withStep(new WallTouchStep());
                plan.begin();
                return plan.getOutput(input);
            }

            plan = new Plan().withStep(new DescendFromWallStep());
            plan.begin();
            return plan.getOutput(input);
        }

        if (car.position.z < NEEDS_LANDING_HEIGHT || ArenaModel.isBehindGoalLine(car.position)) {
            return Optional.empty();
        }

        if (plan == null || plan.isComplete()) {
            plan = planRotation(car, facingFn, input.team);
            plan.begin();
        }

        return plan.getOutput(input);
    }

    private static Plan planRotation(CarData car, Function<AgentInput, Vector2> facingFn, Bot.Team team) {

        CarOrientation current = car.orientation;
        boolean pitchFirst = Math.abs(car.spin.pitchRate) > 1 || Math.abs(current.roofVector.z) > SIN_45;

        return new Plan()
                .withStep(pitchFirst ? new PitchToPlaneStep(UP_VECTOR, true) : new YawToPlaneStep(UP_VECTOR, true))
                .withStep(new RollToPlaneStep(UP_VECTOR))
                .withStep(new YawToPlaneStep(input -> getFacingPlane(facingFn.apply(input))));
    }

    private static Vector3 getFacingPlane(Vector2 desiredFacing) {
        return new Vector3(-desiredFacing.y, -desiredFacing.x, 0);
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Landing gracefully " + (plan != null ? "(" + plan.getSituation() + ")" : "");
    }
}
