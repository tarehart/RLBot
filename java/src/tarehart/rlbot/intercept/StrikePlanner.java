package tarehart.rlbot.intercept;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.strikes.MidairStrikeStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.tuning.BotLog;

import java.util.Optional;

public class StrikePlanner {

    public static Optional<Plan> planImmediateLaunch(CarData car, Intercept intercept) {

        double height = intercept.getSpace().getZ();
        StrikeProfile.Style strikeStyle = intercept.getStrikeProfile().style;
        if (strikeStyle == StrikeProfile.Style.AERIAL) {
            AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing Aerial!", car.getPlayerIndex());

                double groundDistance = car.getPosition().flatten().distance(intercept.getSpace().flatten());
                double radiansForTilt = Math.atan2(height, groundDistance) + MidairStrikeStep.UPWARD_VELOCITY_MAINTENANCE_ANGLE;

                double tiltBackSeconds = radiansForTilt * .35;

                if (Duration.Companion.between(car.getTime(), intercept.getTime()).getSeconds() > 1.5 && intercept.getSpace().getZ() > 10) {
                    return Optional.of(SetPieces.performDoubleJumpAerial(tiltBackSeconds * .8));
                }
                return Optional.of(SetPieces.performAerial(tiltBackSeconds));
            }
            return Optional.empty();
        }

        if (strikeStyle == StrikeProfile.Style.JUMP_HIT) {
            LaunchChecklist checklist = AirTouchPlanner.checkJumpHitReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing JumpHit!", car.getPlayerIndex());
                return Optional.of(SetPieces.performJumpHit(height));
            }
            return Optional.empty();
        }

        if (strikeStyle == StrikeProfile.Style.FLIP_HIT) {
            LaunchChecklist checklist = AirTouchPlanner.checkFlipHitReadiness(car, intercept);
            if (checklist.readyToLaunch()) {
                BotLog.println("Performing FlipHit!", car.getPlayerIndex());
                return Optional.of(SetPieces.frontFlip());
            }
            return Optional.empty();
        }

        return Optional.empty();
    }
}
