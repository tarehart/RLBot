package tarehart.rlbot.bots

import tarehart.rlbot.TacticalBundle

interface BundleListener {
    fun processBundle(bundle: TacticalBundle)
}