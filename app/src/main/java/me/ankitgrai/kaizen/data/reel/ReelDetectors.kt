package me.ankitgrai.kaizen.data.reel

internal class ReelDetectors(
    private val instagram: ReelPlayerDetector = instagramReelDetector(),
    private val youtube: ReelPlayerDetector = youtubeShortsDetector(),
    private val tiktok: ReelPlayerDetector = tiktokReelDetector(),
) {
    fun forPackage(packageName: String): ReelPlayerDetector? = when (packageName) {
        ReelApps.INSTAGRAM -> instagram
        ReelApps.YOUTUBE -> youtube
        ReelApps.TIKTOK, ReelApps.TIKTOK_LITE -> tiktok
        else -> null
    }
}
