package me.ankitgrai.kaizen.data.reel

import android.content.Context
import android.content.Intent

enum class ReelFamily {
    Instagram,
    YouTube,
    TikTok,
}

data class ReelAppChoice(
    val packageName: String,
    val label: String,
)

object ReelApps {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val TIKTOK = "com.zhiliaoapp.musically"
    const val TIKTOK_LITE = "com.ss.android.ugc.trill"

    val official: Set<String> = setOf(INSTAGRAM, YOUTUBE, TIKTOK, TIKTOK_LITE)

    fun family(packageName: String): ReelFamily? {
        val name = packageName.lowercase()
        return when {
            ".instagram" in name -> ReelFamily.Instagram
            ".youtube" in name -> ReelFamily.YouTube
            ".musically" in name || ".trill" in name || "tiktok" in name -> ReelFamily.TikTok
            else -> null
        }
    }

    fun installedChoices(context: Context): List<ReelAppChoice> {
        val packageManager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcher, 0)
            .mapNotNull { resolve ->
                val packageName = resolve.activityInfo?.packageName ?: return@mapNotNull null
                if (family(packageName) == null) return@mapNotNull null
                val label = resolve.loadLabel(packageManager)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: packageName
                ReelAppChoice(packageName = packageName, label = label)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy<ReelAppChoice> { it.label.lowercase() }.thenBy { it.packageName })
    }
}
