package tarehart.rlbot.planning

enum class PlanGuidance {
    CONTINUE,
    CANCEL,
    STEP_SUCCEEDED, // Some plans will take this as a cue to cancel themselves, some will proceed with the next step.
}
