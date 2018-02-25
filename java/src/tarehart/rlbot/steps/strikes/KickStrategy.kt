package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3

interface KickStrategy {
    fun getKickDirection(input: AgentInput): Vector3?
    fun getKickDirection(input: AgentInput, ballPosition: Vector3): Vector3?
    fun getKickDirection(input: AgentInput, ballPosition: Vector3, easyKick: Vector3): Vector3?
    fun looksViable(car: CarData, ballPosition: Vector3): Boolean
}
