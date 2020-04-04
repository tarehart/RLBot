package tarehart.rlbot.steps

import tarehart.rlbot.planning.PlanGuidance
import java.awt.Graphics2D


abstract class StandardStep : Step {
    override fun canInterrupt(): Boolean {
        return true
    }

    override fun getPlanGuidance(): PlanGuidance {
        return PlanGuidance.CONTINUE
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    override fun reset() {
        // Do nothing
    }
}
