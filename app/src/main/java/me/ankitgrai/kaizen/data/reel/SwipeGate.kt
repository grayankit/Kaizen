package me.ankitgrai.kaizen.data.reel

internal class SwipeGate(
    private val gapMs: Long = 500L,
) {
    private val lastCountedAt = HashMap<String, Long>()

    fun allow(packageName: String, nowMs: Long): Boolean {
        val previous = lastCountedAt[packageName]
        if (previous != null && nowMs - previous < gapMs) return false
        lastCountedAt[packageName] = nowMs
        return true
    }
}
