package tarehart.rlbot.planning;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.tuning.BotLog;

import java.util.Optional;

public class InterceptPlanner {

    public static Optional<Plan> planImmediateLaunch(CarData car, SpaceTime intercept) {

        double height = intercept.space.z;
        if (height > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
            AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing Aerial!", car.playerIndex);

                double groundDistance = car.position.flatten().distance(intercept.space.flatten());
                double radiansForTilt = Math.atan2(height, groundDistance);

                return Optional.of(SetPieces.performAerial(radiansForTilt * .7));
            }
            return Optional.empty();
        }

        if (height > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD && AirTouchPlanner.isJumpHitAccessible(car, intercept)) {
            LaunchChecklist checklist = AirTouchPlanner.checkJumpHitReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing JumpHit!", car.playerIndex);
                return Optional.of(SetPieces.performJumpHit(height));
            }
            return Optional.empty();
        }

        if (height > AirTouchPlanner.NEEDS_FRONT_FLIP_THRESHOLD && AirTouchPlanner.isFlipHitAccessible(car, intercept)) {
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
