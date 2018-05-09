package tarehart.rlbot

import tarehart.rlbot.math.Clamper

class AgentOutput : rlbot.ControllerState {

    // 0 is straight, -1 is hard left, 1 is hard right.
    var steer: Double = 0.0
        private set

    // -1 for front flip, 1 for back flip
    var pitch: Double = 0.0
        private set

    var yaw: Double = 0.0
        private set

    var roll: Double = 0.0
        private set

    // -1 is reverse, 0 is idle, 1 is full
    var throttle: Double = 0.0
        private set

    var jumpDepressed: Boolean = false
        private set

    var boostDepressed: Boolean = false
        private set

    var slideDepressed: Boolean = false
        private set

    fun withSteer(steeringTilt: Double): AgentOutput {
        this.steer = Clamper.clamp(steeringTilt, -1.0, 1.0)
        return this
    }

    fun withPitch(pitchTilt: Double): AgentOutput {
        this.pitch = Clamper.clamp(pitchTilt, -1.0, 1.0)
        return this
    }

    fun withYaw(yawTilt: Double): AgentOutput {
        this.yaw = Clamper.clamp(yawTilt, -1.0, 1.0)
        return this
    }

    fun withRoll(rollTilt: Double): AgentOutput {
        this.roll = Math.max(-1.0, Math.min(1.0, rollTilt))
        return this
    }

    fun withThrottle(throttle: Double): AgentOutput {
        this.throttle = Clamper.clamp(throttle, -1.0, 1.0)
        return this
    }

    fun withJump(jumpDepressed: Boolean): AgentOutput {
        this.jumpDepressed = jumpDepressed
        return this
    }

    fun withBoost(boostDepressed: Boolean): AgentOutput {
        this.boostDepressed = boostDepressed
        return this
    }

    fun withSlide(slideDepressed: Boolean): AgentOutput {
        this.slideDepressed = slideDepressed
        return this
    }

    fun withJump(): AgentOutput {
        this.jumpDepressed = true
        return this
    }

    fun withBoost(): AgentOutput {
        this.boostDepressed = true
        return this
    }

    fun withSlide(): AgentOutput {
        this.slideDepressed = true
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AgentOutput

        if (steer != other.steer) return false
        if (pitch != other.pitch) return false
        if (yaw != other.yaw) return false
        if (roll != other.roll) return false
        if (throttle != other.throttle) return false
        if (jumpDepressed != other.jumpDepressed) return false
        if (boostDepressed != other.boostDepressed) return false
        if (slideDepressed != other.slideDepressed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = steer.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + roll.hashCode()
        result = 31 * result + throttle.hashCode()
        result = 31 * result + jumpDepressed.hashCode()
        result = 31 * result + boostDepressed.hashCode()
        result = 31 * result + slideDepressed.hashCode()
        return result
    }

    override fun getYaw(): Float {
        return yaw.toFloat()
    }

    override fun getSteer(): Float {
        return steer.toFloat()
    }

    override fun getThrottle(): Float {
        return throttle.toFloat()
    }

    override fun getPitch(): Float {
        return pitch.toFloat()
    }

    override fun getRoll(): Float {
        return roll.toFloat()
    }

    override fun holdHandbrake(): Boolean {
        return slideDepressed
    }

    override fun holdBoost(): Boolean {
        return boostDepressed
    }

    override fun holdJump(): Boolean {
        return jumpDepressed
    }
}
