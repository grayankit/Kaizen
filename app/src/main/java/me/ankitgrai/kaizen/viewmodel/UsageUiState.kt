package me.ankitgrai.kaizen.viewmodel

import java.time.LocalDate
import me.ankitgrai.kaizen.data.usage.DailyAppUsage
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.data.usage.HourlyDeviceUsage
import me.ankitgrai.kaizen.ui.components.AppUsage

data class UsageUiState(
    val isLoading: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val apps: List<AppUsage> = emptyList(),
    val installedApps: List<AppUsage> = emptyList(),
    val totalUsageMillis: Long = 0L,
    val yesterdayUsageMillis: Long? = null,
    val error: String? = null,
)

data class AppDetailsUiState(
    val isLoading: Boolean = false,
    val history: List<DailyAppUsage> = emptyList(),
    val error: String? = null,
)

data class DayAppsUiState(
    val date: LocalDate? = null,
    val isLoading: Boolean = false,
    val apps: List<AppUsage> = emptyList(),
)

data class PeriodAppsUiState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val apps: List<AppUsage> = emptyList(),
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val days: List<DailyDeviceUsage> = emptyList(),
    val error: String? = null,
)

data class DayHoursUiState(
    val date: LocalDate? = null,
    val isLoading: Boolean = false,
    val hours: List<HourlyDeviceUsage> = emptyList(),
    val available: Boolean = false,
)
