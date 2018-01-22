package tarehart.rlbot

import rlbot.api.GameData

class AgentOutput {

    // 0 is straight, -1 is hard left, 1 is hard right.
    var steer: Double = 0.0
        private set

    // -1 for front flip, 1 for back flip
    var pitch: Double = 0.0
        private set

    var roll: Double = 0.0
        private set

    // 0 is none, 1 is full
    private var acceleration: Double = 0.0
    private var deceleration: Double = 0.0

    private var jumpDepressed: Boolean = false
    private var boostDepressed: Boolean = false
    private var slideDepressed: Boolean = false

    fun withSteer(steeringTilt: Double): AgentOutput {
        this.steer = Math.max(-1.0, Math.min(1.0, steeringTilt))
        return this
    }

    fun withPitch(pitchTilt: Double): AgentOutput {
        this.pitch = Math.max(-1.0, Math.min(1.0, pitchTilt))
        return this
    }

    fun withRoll(rollTilt: Double): AgentOutput {
        this.roll = Math.max(-1.0, Math.min(1.0, rollTilt))
        return this
    }

    fun withAcceleration(acceleration: Double): AgentOutput {
        this.acceleration = Math.max(0.0, Math.min(1.0, acceleration))
        return this
    }

    fun withDeceleration(deceleration: Double): AgentOutput {
        this.deceleration = Math.max(0.0, Math.min(1.0, deceleration))
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

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as AgentOutput?

        if (java.lang.Double.compare(that!!.steer, steer) != 0) return false
        if (java.lang.Double.compare(that.pitch, pitch) != 0) return false
        if (java.lang.Double.compare(that.acceleration, acceleration) != 0) return false
        if (java.lang.Double.compare(that.deceleration, deceleration) != 0) return false
        if (jumpDepressed != that.jumpDepressed) return false
        return if (boostDepressed != that.boostDepressed) false else slideDepressed == that.slideDepressed
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long
        temp = java.lang.Double.doubleToLongBits(steer)
        result = (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(pitch)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(acceleration)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(deceleration)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        result = 31 * result + if (jumpDepressed) 1 else 0
        result = 31 * result + if (boostDepressed) 1 else 0
        result = 31 * result + if (slideDepressed) 1 else 0
        return result
    }

    fun toControllerState(): GameData.ControllerState {
        return GameData.ControllerState.newBuilder()
                .setThrottle((acceleration - deceleration).toFloat())
                .setSteer(steer.toFloat())
                .setYaw(if (slideDepressed) 0f else steer.toFloat())
                .setRoll(if (roll != 0.0) roll.toFloat() else if (slideDepressed) steer.toFloat() else 0f)
                .setPitch(pitch.toFloat())
                .setBoost(boostDepressed)
                .setHandbrake(slideDepressed)
                .setJump(jumpDepressed)
                .build()
    }
}
