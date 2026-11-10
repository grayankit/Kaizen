package me.ankitgrai.kaizen.data.reel

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.settings.ScreenTimeSettings

class ReelAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val detectors = ReelDetectors()
    private val gate = SwipeGate()
    private val repository by lazy { ReelRepository(this) }

    override fun onServiceConnected() {
        val info = serviceInfo ?: return
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED
        info.packageNames = ReelApps.packages.toTypedArray()
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = 0
        info.notificationTimeout = 0
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in ReelApps.packages) return
        if (packageName !in ScreenTimeSettings.readReelApps(this)) return
        val className = event.className?.toString() ?: return
        val now = SystemClock.elapsedRealtime()
        val sample = ScrollSample(
            className = className,
            scrollDeltaX = event.scrollDeltaX,
            scrollDeltaY = event.scrollDeltaY,
            scrollY = event.scrollY,
            fromIndex = event.fromIndex,
            toIndex = event.toIndex,
            eventTimeMs = now,
            screenHeightPx = resources.displayMetrics.heightPixels,
        )
        val detector = detectors.forPackage(packageName) ?: return
        val counted = detector.isPlayerSwipe(sample) && gate.allow(packageName, now)
        logScroll(packageName, className, sample.scrollDeltaY, counted)
        if (!counted) return
        scope.launch {
            runCatching {
                if (packageName in ScreenTimeSettings.readReelApps(applicationContext)) {
                    repository.recordSwipe(packageName)
                }
            }.onFailure { error ->
                Log.w(TAG, "Could not save a reel swipe", error)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun logScroll(
        packageName: String,
        className: String,
        scrollDeltaY: Int,
        counted: Boolean,
    ) {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) return
        Log.d(TAG, "scroll package=$packageName class=$className dy=$scrollDeltaY counted=$counted")
    }

    private companion object {
        const val TAG = "ReelCounter"
    }
}
