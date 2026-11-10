package me.ankitgrai.kaizen.data.reel

object ReelApps {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val TIKTOK = "com.zhiliaoapp.musically"
    const val TIKTOK_LITE = "com.ss.android.ugc.trill"

    val defaults: Map<String, String> = mapOf(
        INSTAGRAM to "Instagram",
        YOUTUBE to "YouTube",
        TIKTOK to "TikTok",
        TIKTOK_LITE to "TikTok",
    )

    val packages: Set<String> = defaults.keys
}

// Keep res/xml/reel_accessibility_service.xml packageNames in sync with ReelApps.packages.
