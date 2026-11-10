package me.ankitgrai.kaizen.data.reel

/**
 * Starting class-name needles for the Instagram Reels player.
 * Confirm on a phone. An emulator cannot prove them.
 * Home, Stories, comments, and search are ignored by name, not by reading text.
 */
internal val INSTAGRAM_PLAYER_CLASSES = listOf(
    "viewpager",
    "clips",
)

internal val INSTAGRAM_IGNORED_CLASSES = listOf(
    "story",
    "comment",
    "search",
    "explore",
    "direct",
)

internal fun instagramReelDetector(
    pages: PageSwipeTracker = PageSwipeTracker(),
) = ClassNameReelDetector(
    playerClassNames = INSTAGRAM_PLAYER_CLASSES,
    ignoredClassNames = INSTAGRAM_IGNORED_CLASSES,
    pages = pages,
)
