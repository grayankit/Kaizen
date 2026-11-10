package me.ankitgrai.kaizen.data.reel

internal data class ScrollSample(
    val className: String,
    val scrollDeltaX: Int,
    val scrollDeltaY: Int,
    val scrollY: Int,
    val fromIndex: Int,
    val toIndex: Int,
    val eventTimeMs: Long,
    val screenHeightPx: Int,
)
