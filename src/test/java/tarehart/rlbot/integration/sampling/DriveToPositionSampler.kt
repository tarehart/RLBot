package tarehart.rlbot.integration.sampling


import org.junit.Test
import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.*
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.input.CarSpin
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.NullRenderer
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.io.File
import java.util.*

class DriveToPositionSampler: StateSettingAbstractTest() {

    /*
    penaltySeconds =

      0.2786 * angle +
     -0.101  * angleSquared +
      0.0326 * angleCubed +
      0.0151 * unpleasantSpeed

    Correlation coefficient                  0.9147
    Mean absolute error                      0.0927
    Root mean squared error                  0.2236
    Relative absolute error                 19.8633 %
    Root relative squared error             40.387  %
    Total Number of Instances             3571


This was achieved by loading the CSV in https://www.cs.waikato.ac.nz/ml/weka/ and doing a linear regression.

     */

    @Test
    fun run() {
        runTestCase(StateSettingTestCase(
                GameState()
                        .withGameInfoState(GameInfoState().withGameSpeed(2F))
                        .withBallState(BallState().withPhysics(PhysicsState().withLocation(StateVector(0, -80, 5)))),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(10000))),
                Plan(Posture.OVERRIDE).withStep(DriveSamplerStep())))
    }
}

class DriveSamplerStep: StandardStep() {
    override val situation = ""
    var target = Vector3()
    var speed = 0F
    var distance = 0F
    var angle = 0F
    var boost = 0F
    var startTime = GameTime.zero()
    var begun = false
    val random = Random()

    val file = File("driveSampler.csv").printWriter()

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {
        val car = bundle.agentInput.myCarData
        if (!begun || car.position.distance(target) < 1) {

            if (begun) {
                val car = CarData(Vector3(0, 0, 0), Vector3(0, speed, 0), CarOrientation(Vector3(0, 1, 0), Vector3.UP),
                        CarSpin(0F, 0F, 0F, Vector3.ZERO), boost, false, Team.BLUE, 0, car.time, 0,
                        true, false, "", NullRenderer(), true, CarHitbox.OCTANE)

                val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6), boost, 0)
                val expectedTime = distancePlot.getTravelTime(distance)?.seconds ?: 0F
                val unpleasantSpeed = car.velocity.magnitude() - car.velocity.dotProduct((target - car.position).normaliseCopy())

                val elapsed = car.time - startTime
                println("speed $speed distance $distance angle $angle boost $boost result: $elapsed")
                file.flush()
                file.println("$speed,${speed * speed},$distance,$angle,${angle * angle},${angle * angle * angle},$boost," +
                        "$unpleasantSpeed,${unpleasantSpeed * unpleasantSpeed},${elapsed.seconds - expectedTime}")
            } else {
                file.println("speed,speedSquared,distance,angle,angleSquared,angleCubed,boost,unpleasantSpeed,unpleasantSpeedSquared,penaltySeconds")
            }

            speed = random.nextFloat() * AccelerationModel.SUPERSONIC_SPEED
            distance = random.nextFloat() * 40 + 20
            angle = random.nextFloat() * BotMath.PI
            boost = random.nextFloat() * 100
            target = VectorUtil.rotateVector(Vector2(0, distance), angle).withZ(0)

            begun = true
            startTime = car.time
            RLBotDll.setGameState(GameState().withCarState(0,
                    CarState().withPhysics(PhysicsState(
                            StateVector(0, 0, 0),
                            DesiredRotation(0F, BotMath.PI / 2, 0F),
                            StateVector(0, speed, 0),
                            StateVector(0, 0, 0)
                    )).withBoostAmount(boost))
                    .buildPacket())
        }
        return SteerUtil.steerTowardGroundPosition(car, target)
    }

}
