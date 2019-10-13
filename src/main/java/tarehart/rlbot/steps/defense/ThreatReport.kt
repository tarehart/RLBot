package tarehart.rlbot.steps.defense

data class ThreatReport(
        val enemyMightBoom: Boolean,
        val enemyShotAligned: Boolean,
        val enemyWinsRace: Boolean,
        val enemyHasBreakaway: Boolean,
        val ballIsBehindUs: Boolean) {


    fun looksSerious(): Boolean {
        if (enemyMightBoom && enemyShotAligned && enemyWinsRace) {
            return true
        }

        if (enemyHasBreakaway) {
            return true
        }

        return false
    }
}
