package tarehart.rlbot.planning;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.tuning.BotLog;

import java.util.Optional;

public class InterceptPlanner {

    public static Optional<Plan> planImmediateLaunch(CarData car, SpaceTime intercept) {


        if (intercept.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
            AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing Aerial!", car.playerIndex);
                return Optional.of(SetPieces.performAerial());
            }
            return Optional.empty();
        }

        if (intercept.space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD && AirTouchPlanner.isJumpHitAccessible(car, intercept)) {
            LaunchChecklist checklist = AirTouchPlanner.checkJumpHitReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing JumpHit!", car.playerIndex);
                return Optional.of(SetPieces.performJumpHit(intercept.space.z));
            }
            return Optional.empty();
        }

        if (intercept.space.z > AirTouchPlanner.NEEDS_FRONT_FLIP_THRESHOLD && AirTouchPlanner.isFlipHitAccessible(car, intercept)) {
            LaunchChecklist checklist = AirTouchPlanner.checkFlipHitReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing FlipHit!", car.playerIndex);
                return Optional.of(SetPieces.frontFlip());
            }
            return Optional.empty();
        }

        return Optional.empty();
    }
}
