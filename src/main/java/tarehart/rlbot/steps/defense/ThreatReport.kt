package tarehart.rlbot.steps.defense

data class ThreatReport(
        val enemyMightBoom: Boolean,
        val enemyShotAligned: Boolean,
        val enemyWinsRace: Boolean,
        val enemyHasBreakaway: Boolean,
        val ballIsBehindUs: Boolean,
        val enemyDribbling: Boolean,
        val challengeImminent: Boolean) {


    fun looksSerious(): Boolean {
        if (enemyMightBoom && enemyShotAligned && enemyWinsRace) {
            return true
        }

        if (enemyHasBreakaway) {
            return true
        }

        if (enemyDribbling && enemyShotAligned) {
            return true
        }

        if (challengeImminent) {
            return true
        }

        return false
    }
}
