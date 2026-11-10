package me.ankitgrai.kaizen.data.reel

internal class ClassNameReelDetector(
    private val playerClassNames: List<String>,
    private val ignoredClassNames: List<String>,
    private val pages: PageSwipeTracker = PageSwipeTracker(),
) : ReelPlayerDetector {
    override fun isPlayerSwipe(sample: ScrollSample): Boolean {
        val name = sample.className.lowercase()
        if (ignoredClassNames.any { it.lowercase() in name }) return false
        if (playerClassNames.none { it.lowercase() in name }) return false
        return pages.isOnePageVertical(sample, countIndexStep = "viewpager" in name)
    }
}
