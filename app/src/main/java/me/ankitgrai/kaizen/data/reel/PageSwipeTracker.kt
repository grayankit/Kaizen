package me.ankitgrai.kaizen.data.reel

import kotlin.math.abs

internal class PageSwipeTracker(
    private val pageFraction: Float = 0.5f,
    private val accumulateWindowMs: Long = 400L,
) {
    private var pendingY = 0
    private var pendingAt = Long.MIN_VALUE
    private var lastScrollY: Int? = null

    fun isOnePageVertical(sample: ScrollSample, countIndexStep: Boolean = false): Boolean {
        if (sample.screenHeightPx <= 0) return false
        if (isSideways(sample)) {
            reset()
            return false
        }
        if (countIndexStep && indexMoved(sample)) {
            reset()
            return true
        }
        val delta = verticalDelta(sample) ?: return false
        if (delta == 0) return false
        val expired = sample.eventTimeMs - pendingAt > accumulateWindowMs
        val reversed = pendingY != 0 && (delta > 0) != (pendingY > 0)
        if (expired || reversed) pendingY = 0
        pendingY += delta
        pendingAt = sample.eventTimeMs
        val page = (sample.screenHeightPx * pageFraction).toInt().coerceAtLeast(1)
        if (abs(pendingY) >= page) {
            reset()
            return true
        }
        return false
    }

    private fun reset() {
        pendingY = 0
        pendingAt = Long.MIN_VALUE
        lastScrollY = null
    }

    private fun verticalDelta(sample: ScrollSample): Int? {
        if (sample.scrollDeltaY != UNDEFINED) return sample.scrollDeltaY
        if (sample.scrollDeltaX != UNDEFINED) return 0
        if (sample.scrollY == UNDEFINED) return null
        val previous = lastScrollY
        lastScrollY = sample.scrollY
        return previous?.let { sample.scrollY - it }
    }

    private fun isSideways(sample: ScrollSample): Boolean {
        val dx = sample.scrollDeltaX
        val dy = sample.scrollDeltaY
        if (dx == UNDEFINED || dy == UNDEFINED) return false
        return abs(dx) > abs(dy) && abs(dx) > 8
    }

    private fun indexMoved(sample: ScrollSample): Boolean {
        if (sample.fromIndex < 0 || sample.toIndex < 0) return false
        return sample.fromIndex != sample.toIndex
    }

    private companion object {
        const val UNDEFINED = -1
    }
}
