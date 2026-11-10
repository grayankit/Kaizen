package me.ankitgrai.kaizen.data.reel

internal interface ReelPlayerDetector {
    fun isPlayerSwipe(sample: ScrollSample): Boolean
}
