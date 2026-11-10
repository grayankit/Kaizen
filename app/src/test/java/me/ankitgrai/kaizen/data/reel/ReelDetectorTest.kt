package me.ankitgrai.kaizen.data.reel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelDetectorTest {
    @Test
    fun instagramCountsAFullPageReelsSwipe() {
        val detector = instagramReelDetector()
        assertTrue(detector.isPlayerSwipe(sample(className = VIEW_PAGER_2, deltaY = PAGE)))
    }

    @Test
    fun instagramIgnoresHomeFeedStoriesCommentsAndSearch() {
        val detector = instagramReelDetector()
        assertFalse(detector.isPlayerSwipe(sample(className = RECYCLER, deltaY = PAGE)))
        assertFalse(
            detector.isPlayerSwipe(
                sample(className = "com.instagram.ui.widget.story.StoryViewer", deltaY = PAGE),
            ),
        )
        assertFalse(
            detector.isPlayerSwipe(
                sample(className = VIEW_PAGER_2, deltaX = PAGE, deltaY = 12),
            ),
        )
        assertFalse(
            detector.isPlayerSwipe(
                sample(className = "android.widget.ScrollView", deltaY = PAGE),
            ),
        )
    }

    @Test
    fun chunkedVerticalMoveCountsOnce() {
        val detector = instagramReelDetector()
        assertFalse(detector.isPlayerSwipe(sample(className = VIEW_PAGER_2, deltaY = 600, at = 1_000)))
        assertTrue(detector.isPlayerSwipe(sample(className = VIEW_PAGER_2, deltaY = 600, at = 1_200)))
        assertFalse(detector.isPlayerSwipe(sample(className = VIEW_PAGER_2, deltaY = 40, at = 1_250)))
    }

    @Test
    fun youtubeCountsShortsPagerNotHomeFeed() {
        val detector = youtubeShortsDetector()
        assertTrue(
            detector.isPlayerSwipe(
                sample(
                    className = "com.google.android.apps.youtube.app.extensions.reel.ReelWatchPager",
                    deltaY = -PAGE,
                ),
            ),
        )
        assertFalse(detector.isPlayerSwipe(sample(className = RECYCLER, deltaY = PAGE)))
    }

    @Test
    fun tiktokCountsVerticalPagerNotInbox() {
        val detector = tiktokReelDetector()
        assertTrue(
            detector.isPlayerSwipe(
                sample(className = "com.ss.android.ugc.aweme.common.widget.VerticalViewPager", deltaY = PAGE),
            ),
        )
        assertFalse(detector.isPlayerSwipe(sample(className = RECYCLER, deltaY = PAGE)))
        assertFalse(
            detector.isPlayerSwipe(
                sample(className = "com.ss.android.ugc.aweme.inbox.InboxList", deltaY = PAGE),
            ),
        )
    }

    @Test
    fun patchedPackagesUseTheMatchingFamily() {
        assertEquals(ReelFamily.Instagram, ReelApps.family("com.instagram.android"))
        assertEquals(ReelFamily.Instagram, ReelApps.family("app.rvx.android.instagram"))
        assertEquals(ReelFamily.YouTube, ReelApps.family("com.google.android.youtube"))
        assertEquals(ReelFamily.YouTube, ReelApps.family("app.revanced.android.youtube"))
        assertEquals(ReelFamily.YouTube, ReelApps.family("com.google.android.apps.youtube.music"))
        assertEquals(ReelFamily.TikTok, ReelApps.family("com.zhiliaoapp.musically"))
        assertEquals(ReelFamily.TikTok, ReelApps.family("com.zhiliaoapp.musically.go"))
        assertEquals(ReelFamily.TikTok, ReelApps.family("com.ss.android.ugc.trill"))
        assertEquals(ReelFamily.TikTok, ReelApps.family("app.revanced.android.tiktok"))
        assertNull(ReelApps.family("com.example.notes"))
        assertFalse("com.google.android.apps.youtube.music" in ReelApps.official)
        assertTrue(ReelApps.YOUTUBE in ReelApps.official)

        val detectors = ReelDetectors()
        assertTrue(detectors.forPackage("app.revanced.android.youtube") != null)
        assertTrue(detectors.forPackage("app.rvx.android.instagram") != null)
        assertNull(detectors.forPackage("com.example.notes"))
    }

    @Test
    fun gateDropsASecondSwipeInside500ms() {
        val gate = SwipeGate()
        assertTrue(gate.allow(ReelApps.INSTAGRAM, 1_000))
        assertFalse(gate.allow(ReelApps.INSTAGRAM, 1_499))
        assertTrue(gate.allow(ReelApps.INSTAGRAM, 1_500))
        assertTrue(gate.allow(ReelApps.YOUTUBE, 1_200))
    }

    private fun sample(
        className: String,
        deltaY: Int,
        deltaX: Int = 0,
        at: Long = 5_000,
    ) = ScrollSample(
        className = className,
        scrollDeltaX = deltaX,
        scrollDeltaY = deltaY,
        scrollY = 0,
        fromIndex = -1,
        toIndex = -1,
        eventTimeMs = at,
        screenHeightPx = SCREEN,
    )

    private companion object {
        const val SCREEN = 2_000
        const val PAGE = 1_200
        const val VIEW_PAGER_2 = "androidx.viewpager2.widget.ViewPager2"
        const val RECYCLER = "androidx.recyclerview.widget.RecyclerView"
    }
}
