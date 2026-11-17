package me.ankitgrai.kaizen.domain

fun formatDuration(
    milliseconds: Long,
    alwaysShowHours: Boolean = false,
): String {
    val totalMinutes = milliseconds / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0 || alwaysShowHours) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
