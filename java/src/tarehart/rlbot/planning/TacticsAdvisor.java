package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.steps.*;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.RotateAndWaitToClearStep;
import tarehart.rlbot.steps.defense.WhatASaveStep;
import tarehart.rlbot.steps.strikes.*;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.MountWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.util.Optional;

import static tarehart.rlbot.planning.Plan.Posture.ESCAPEGOAL;
import static tarehart.rlbot.planning.Plan.Posture.NEUTRAL;
import static tarehart.rlbot.planning.Plan.Posture.OFFENSIVE;
import static tarehart.rlbot.tuning.BotLog.println;

public class TacticsAdvisor {

    private static final double LOOKAHEAD_SECONDS = 2;
    private static final Duration PLAN_HORIZON = Duration.Companion.ofSeconds(6);

    public TacticsAdvisor() {
    }

    public Plan makePlan(AgentInput input, TacticalSituation situation) {

        if (situation.scoredOnThreat.isPresent()) {
            return new Plan(Plan.Posture.SAVE).withStep(new WhatASaveStep());
        }
//        if (ArenaModel.isBehindGoalLine(input.getMyCarData().getPosition())) {
//            return new Plan(ESCAPEGOAL).withStep(new EscapeTheGoalStep());
//        }
        if(situation.waitToClear) {
            return new Plan(Plan.Posture.WAITTOCLEAR).withStep(new RotateAndWaitToClearStep());
        }
        if (situation.needsDefensiveClear) {
            return new FirstViableStepPlan(Plan.Posture.CLEAR)
                    .withStep(new DirectedNoseHitStep(new KickAwayFromOwnGoal())) // TODO: make these fail if you have to drive through a goal post
                    .withStep(new DirectedSideHitStep(new KickAwayFromOwnGoal()))
                    .withStep(new EscapeTheGoalStep())
                    .withStep(new GetOnDefenseStep());
        }
        if (situation.forceDefensivePosture) {
            double secondsToOverrideFor = 0.25;
            return new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep(secondsToOverrideFor));
        }

        Vector3 ownGoalCenter = GoalUtil.getOwnGoal(input.getTeam()).getCenter();
        Vector3 interceptPosition = situation.expectedContact.map(Intercept::getSpace).orElse(input.getBallPosition());
        Vector3 toOwnGoal = ownGoalCenter.minus(interceptPosition);
        Vector3 interceptModifier = toOwnGoal.normaliseCopy();

        if (situation.shotOnGoalAvailable) {

            return new FirstViableStepPlan(Plan.Posture.OFFENSIVE)
                    .withStep(new DirectedNoseHitStep(new KickAtEnemyGoal()))
                    .withStep(new DirectedSideHitStep(new KickAtEnemyGoal()))
                    .withStep(new CatchBallStep())
                    .withStep(new GetOnOffenseStep());
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);

        final double raceResult = calculateRaceResult(
                situation.expectedContact.map(Intercept::getTime),
                situation.expectedEnemyContact.map(Intercept::getTime));

        if (raceResult > 2) {
            // We can take our sweet time. Now figure out whether we want a directed kick, a dribble, an intercept, a catch, etc
            return makePlanWithPlentyOfTime(input, situation, ballPath);
        }

        if (raceResult > -.3) {

            if (situation.enemyOffensiveApproachError.map(e -> e < Math.PI / 3).orElse(false)) {

                // Enemy is threatening us

                if (getYAxisWrongSidedness(input) < 0) {

                    // Consider this to be a 50-50. Go hard for the intercept
                    return new Plan(Plan.Posture.OFFENSIVE).withStep(new InterceptStep(interceptModifier));
                } else {
                    // We're not in a good position to go for a 50-50. Get on defense.
                    return new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
                }
            } else {
                // Doesn't matter if enemy wins the race, they are out of position.
                return makePlanWithPlentyOfTime(input, situation, ballPath);
            }
        }

        // The enemy is probably going to get there first.
        if (situation.enemyOffensiveApproachError.map(e -> e < Math.PI / 3).orElse(false) && situation.distanceBallIsBehindUs > -50) {
            // Enemy can probably shoot on goal, so get on defense
            return new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
        } else {
            // Enemy is just gonna hit it for the sake of hitting it, presumably. Let's try to stay on offense if possible.
            // TODO: make sure we don't own-goal it with this
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new GetOnOffenseStep()).withStep(new InterceptStep(new Vector3()));
        }

    }

    public static double calculateRaceResult(SpaceTime myIntercept, CarData enemyCar, BallPath ballPath) {
        Optional<Intercept> enemyIntercept = getSoonestIntercept(enemyCar, ballPath);

        return calculateRaceResult(Optional.of(myIntercept.getTime()), enemyIntercept.map(Intercept::getTime));
    }

    private static double calculateRaceResult(Optional<GameTime> ourContact, Optional<GameTime> enemyContact) {
        final double raceResult;
        if (!enemyContact.isPresent()) {
            return 3;
        } else if (!ourContact.isPresent()) {
            raceResult = -3;
        } else {
            raceResult = Duration.Companion.between(ourContact.get(), enemyContact.get()).getSeconds();
        }

        return raceResult;
    }

    private Plan makePlanWithPlentyOfTime(AgentInput input, TacticalSituation situation, BallPath ballPath) {

        CarData car = input.getMyCarData();

        if (!generousShotAngle(GoalUtil.getEnemyGoal(car.getTeam()), situation.expectedContact, car.getPlayerIndex())) {
            Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(car, ballPath, car.getBoost());
            if (catchOpportunity.isPresent()) {
                return new Plan(Plan.Posture.OFFENSIVE).withStep(new CatchBallStep());
            }
        }

        if (DribbleStep.canDribble(input, false) && input.getBallVelocity().magnitude() > 15) {
            println("Beginning dribble", input.getPlayerIndex());
            return new Plan(OFFENSIVE).withStep(new DribbleStep());
        }  else if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new MountWallStep()).withStep(new WallTouchStep()).withStep(new DescendFromWallStep());
        } else if (generousShotAngle(GoalUtil.getEnemyGoal(car.getTeam()), situation.expectedContact, car.getPlayerIndex()) &&
                DirectedNoseHitStep.canMakeDirectedKick(input, new KickAtEnemyGoal())) {
            return new FirstViableStepPlan(Plan.Posture.OFFENSIVE)
                    .withStep(new DirectedNoseHitStep(new KickAtEnemyGoal()))
                    .withStep(new DirectedNoseHitStep(new FunnelTowardEnemyGoal()))
                    .withStep(new GetOnOffenseStep());
        } else if (car.getBoost() < 50) {
            return new Plan().withStep(new GetBoostStep());
        } else if (getYAxisWrongSidedness(input) > 0) {
            println("Getting behind the ball", input.getPlayerIndex());
            return new Plan(NEUTRAL).withStep(new GetOnOffenseStep());
        } else {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new InterceptStep(new Vector3()));
        }
    }

    public static boolean generousShotAngle(Goal goal, Vector2 expectedContact, int playerIndex) {

        Vector2 goalCenter = goal.getCenter().flatten();
        Vector2 ballToGoal = goalCenter.minus(expectedContact);
        boolean generousAngle = Vector2.Companion.angle(goalCenter, ballToGoal) < Math.PI / 4;
        boolean generousTriangle = measureShotTriangle(goal, expectedContact, playerIndex) > Math.PI / 12;

        return generousAngle || generousTriangle;
    }

    private static boolean generousShotAngle(Goal goal, Optional<Intercept> expectedContact, int playerIndex) {
        return expectedContact
                .map(contact -> generousShotAngle(goal, contact.getSpace().flatten(), playerIndex))
                .orElse(false);
    }

    private static double measureShotTriangle(Goal goal, Vector2 position, int playerIndex) {
        Vector2 toRightPost = goal.getRightPost().flatten().minus(position);
        Vector2 toLeftPost = goal.getLeftPost().flatten().minus(position);

        double angle = Vector2.Companion.angle(toLeftPost, toRightPost);
        // BotLog.println(String.format("Shot angle: %.2f", angle), playerIndex);

        return angle;
    }

    public TacticalSituation assessSituation(AgentInput input, BallPath ballPath, Plan currentPlan) {

        Optional<Intercept> enemyIntercept = input.getEnemyCarData()
                .flatMap(car -> getSoonestIntercept(car, ballPath));

        Optional<Intercept> ourIntercept = getSoonestIntercept(input.getMyCarData(), ballPath);

        Optional<ZonePlan> zonePlan = ZoneTelemetry.get(input.getTeam());
        CarData myCar = input.getMyCarData();
        Optional<CarData> opponentCar = input.getEnemyCarData();

        BallSlice futureBallMotion = ballPath.getMotionAt(input.getTime().plusSeconds(LOOKAHEAD_SECONDS)).orElse(ballPath.getEndpoint());

        TacticalSituation situation = new TacticalSituation();
        situation.expectedContact = ourIntercept;
        situation.expectedEnemyContact = enemyIntercept;
        situation.ownGoalFutureProximity = VectorUtil.INSTANCE.flatDistance(GoalUtil.getOwnGoal(input.getTeam()).getCenter(), futureBallMotion.getSpace());
        situation.distanceBallIsBehindUs = measureOutOfPosition(input);
        situation.enemyOffensiveApproachError = situation.expectedEnemyContact.map(contact -> measureEnemyApproachError(input, contact.toSpaceTime()));
        double enemyGoalY = GoalUtil.getEnemyGoal(input.getTeam()).getCenter().getY();
        situation.distanceFromEnemyBackWall = Math.abs(enemyGoalY - futureBallMotion.getSpace().getY());
        situation.distanceFromEnemyCorner = getDistanceFromEnemyCorner(futureBallMotion, enemyGoalY);
        situation.futureBallMotion = futureBallMotion;

        situation.scoredOnThreat = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.getTeam()), ballPath);
        situation.needsDefensiveClear = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.getTeam()), ballPath);
        situation.shotOnGoalAvailable = getShotOnGoalAvailable(input.getTeam(), myCar, opponentCar, input.getBallPosition(), ourIntercept, ballPath);
        situation.forceDefensivePosture = getForceDefensivePosture(input.getTeam(), myCar, opponentCar, input.getBallPosition());
        situation.goForKickoff = getGoForKickoff(zonePlan, input.getTeam(), input.getBallPosition());
        situation.waitToClear = getWaitToClear(zonePlan, input);
        situation.currentPlan = Optional.ofNullable(currentPlan);

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry.set(situation, input.getPlayerIndex());

        return situation;
    }

    private double getDistanceFromEnemyCorner(BallSlice futureBallMotion, double enemyGoalY) {
        Vector2 positiveCorner = ArenaModel.CORNER_ANGLE_CENTER;
        double goalSign = Math.signum(enemyGoalY);

        Vector2 corner1 = new Vector2(positiveCorner.getX(), positiveCorner.getY() * goalSign);
        Vector2 corner2 = new Vector2(-positiveCorner.getX(), positiveCorner.getY() * goalSign);

        Vector2 ballFutureFlat = futureBallMotion.getSpace().flatten();

        return Math.min(ballFutureFlat.distance(corner1), ballFutureFlat.distance(corner2));
    }




    private static Optional<Intercept> getSoonestIntercept(CarData car, BallPath ballPath) {
        DistancePlot distancePlot = AccelerationModel.INSTANCE.simulateAcceleration(car, PLAN_HORIZON, car.getBoost());
        return Optional.ofNullable(InterceptStep.Companion.getSoonestIntercept(car, ballPath, distancePlot, new Vector3(), (c, st) -> true));
    }

    private boolean getForceDefensivePosture(Bot.Team team, CarData myCar, Optional<CarData> opponentCar,
                                             Vector3 ballPosition) {
        return opponentCar.map(c -> ZoneUtil.isEnemyOffensiveBreakaway(team, myCar, c, ballPosition)).orElse(false);
    }

    // Really only used for avoiding "Disable Goal Reset" own goals
    private boolean getGoForKickoff(Optional<ZonePlan> zonePlan, Bot.Team team, Vector3 ballPosition) {
        if(zonePlan.isPresent()) {
            if(ballPosition.flatten().magnitudeSquared() == 0) {
                if (team == Bot.Team.BLUE)
                    return zonePlan.get().getMyZone().mainZone == Zone.MainZone.BLUE;
                else
                    return zonePlan.get().getMyZone().mainZone == Zone.MainZone.ORANGE;
            }
        }

        return false;
    }

    // Checks to see if the ball is in the box for a while or if we have a breakaway
    private boolean getShotOnGoalAvailable(Bot.Team team, CarData myCar, Optional<CarData> opponentCar,
                                           Vector3 ballPosition, Optional<Intercept> soonestIntercept, BallPath ballPath) {

        if(myCar.getPosition().distance(ballPosition) < 80 &&
                GoalUtil.ballLingersInBox(GoalUtil.getEnemyGoal(team), ballPath) &&
                generousShotAngle(GoalUtil.getEnemyGoal(myCar.getTeam()), soonestIntercept, myCar.getPlayerIndex())) {
            return true;
        }

        return opponentCar.map(c -> ZoneUtil.isMyOffensiveBreakaway(team, myCar, c, ballPosition)).orElse(false);
    }

    // Checks to see if the ball is in the corner and if the opponent is closer to it
    private boolean getWaitToClear(Optional<ZonePlan> zonePlan, AgentInput input) {
        Vector3 myGoalLocation = GoalUtil.getOwnGoal(input.getTeam()).getCenter();
        double myBallDistance = input.getBallPosition().distance(input.getMyCarData().getPosition());
        double enemyBallDistance = input.getEnemyCarData().map(c -> input.getBallPosition().distance(c.getPosition())).orElse(Double.MAX_VALUE);
        double ballDistanceToGoal = input.getBallPosition().distance(myGoalLocation);
        double myDistanceToGoal = input.getMyCarData().getPosition().distance(myGoalLocation);
        //double enemyDistanceToGoal = input.getEnemyCarData().position.distance(myGoalLocation);

        if(zonePlan.isPresent()
            && (myBallDistance > enemyBallDistance // Enemy is closer
                || myDistanceToGoal > ballDistanceToGoal) // Wrong side of the ball
            && (zonePlan.get().getBallZone().subZone == Zone.SubZone.TOPCORNER
                || zonePlan.get().getBallZone().subZone == Zone.SubZone.BOTTOMCORNER)) {

            if (input.getTeam() == Bot.Team.BLUE)
                return zonePlan.get().getBallZone().mainZone == Zone.MainZone.BLUE;
            else
                return zonePlan.get().getBallZone().mainZone == Zone.MainZone.ORANGE;
        }

        return false;
    }

    private double measureEnemyApproachError(AgentInput input, SpaceTime enemyContact) {

        Optional<CarData> enemyCarData = input.getEnemyCarData();
        if (!enemyCarData.isPresent()) {
            return 0;
        }

        CarData enemyCar = enemyCarData.get();
        Goal myGoal = GoalUtil.getOwnGoal(input.getTeam());
        Vector3 ballToGoal = myGoal.getCenter().minus(enemyContact.getSpace());

        Vector3 carToBall = enemyContact.getSpace().minus(enemyCar.getPosition());

        return Vector2.Companion.angle(ballToGoal.flatten(), carToBall.flatten());
    }


    private double measureOutOfPosition(AgentInput input) {
        CarData car = input.getMyCarData();
        Goal myGoal = GoalUtil.getOwnGoal(input.getTeam());
        Vector3 ballToGoal = myGoal.getCenter().minus(input.getBallPosition());
        Vector3 carToBall = input.getBallPosition().minus(car.getPosition());
        Vector3 wrongSideVector = VectorUtil.INSTANCE.project(carToBall, ballToGoal);
        return wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal));
    }

    public static double getYAxisWrongSidedness(AgentInput input) {
        Vector3 ownGoalCenter = GoalUtil.getOwnGoal(input.getTeam()).getCenter();
        double playerToBallY = input.getBallPosition().getY() - input.getMyCarData().getPosition().getY();
        return playerToBallY * Math.signum(ownGoalCenter.getY());
    }

    public static double getYAxisWrongSidedness(CarData car, Vector3 ball) {
        Vector3 ownGoalCenter = GoalUtil.getOwnGoal(car.getTeam()).getCenter();
        double playerToBallY = ball.getY() - car.getPosition().getY();
        return playerToBallY * Math.signum(ownGoalCenter.getY());
    }

}
