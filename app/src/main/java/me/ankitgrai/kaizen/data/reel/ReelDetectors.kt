package me.ankitgrai.kaizen.data.reel

internal class ReelDetectors(
    private val instagram: ReelPlayerDetector = instagramReelDetector(),
    private val youtube: ReelPlayerDetector = youtubeShortsDetector(),
    private val tiktok: ReelPlayerDetector = tiktokReelDetector(),
) {
    fun forPackage(packageName: String): ReelPlayerDetector? = when (ReelApps.family(packageName)) {
        ReelFamily.Instagram -> instagram
        ReelFamily.YouTube -> youtube
        ReelFamily.TikTok -> tiktok
        null -> null
    }
}
