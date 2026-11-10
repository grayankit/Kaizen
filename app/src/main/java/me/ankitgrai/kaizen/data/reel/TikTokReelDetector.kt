package me.ankitgrai.kaizen.data.reel

/**
 * Starting class-name needles for the TikTok player.
 * Confirm on a phone. An emulator cannot prove them.
 * TikTok obfuscates view classes; add the phone's class substring here if swipes are missed.
 */
internal val TIKTOK_PLAYER_CLASSES = listOf(
    "viewpager",
    "verticalviewpager",
)

internal val TIKTOK_IGNORED_CLASSES = listOf(
    "comment",
    "search",
    "inbox",
    "chat",
    "message",
    "profile",
)

internal fun tiktokReelDetector(
    pages: PageSwipeTracker = PageSwipeTracker(),
) = ClassNameReelDetector(
    playerClassNames = TIKTOK_PLAYER_CLASSES,
    ignoredClassNames = TIKTOK_IGNORED_CLASSES,
    pages = pages,
)
