package me.ankitgrai.kaizen.data.reel

/**
 * Starting class-name needles for the YouTube Shorts player.
 * Confirm on a phone. An emulator cannot prove them.
 * A plain RecyclerView is the home feed. Add a class substring from the phone, not "RecyclerView" alone.
 */
internal val YOUTUBE_PLAYER_CLASSES = listOf(
    "viewpager",
    "shorts",
    "reel",
)

internal val YOUTUBE_IGNORED_CLASSES = listOf(
    "comment",
    "search",
    "engagement",
)

internal fun youtubeShortsDetector(
    pages: PageSwipeTracker = PageSwipeTracker(),
) = ClassNameReelDetector(
    playerClassNames = YOUTUBE_PLAYER_CLASSES,
    ignoredClassNames = YOUTUBE_IGNORED_CLASSES,
    pages = pages,
)
