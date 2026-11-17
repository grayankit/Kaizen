package me.ankitgrai.kaizen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.AppGraph
import me.ankitgrai.kaizen.data.reel.DailyReelCount
import me.ankitgrai.kaizen.data.reel.ReelRepository
import me.ankitgrai.kaizen.data.usage.AppUsageInfo
import me.ankitgrai.kaizen.ui.components.AppUsage
import me.ankitgrai.kaizen.ui.mapping.toAppUsage
import me.ankitgrai.kaizen.ui.mapping.toUnusedApp

class UsageViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = AppGraph.usageRepository(application)
    private val reelRepository = ReelRepository(application)
    private var enabledReelApps: Set<String> = emptySet()

    private val _uiState = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    private val _appDetailsState = MutableStateFlow(AppDetailsUiState())
    val appDetailsState: StateFlow<AppDetailsUiState> = _appDetailsState.asStateFlow()

    private val _dayAppsState = MutableStateFlow(DayAppsUiState())
    val dayAppsState: StateFlow<DayAppsUiState> = _dayAppsState.asStateFlow()

    private val _periodAppsState = MutableStateFlow(PeriodAppsUiState())
    val periodAppsState: StateFlow<PeriodAppsUiState> = _periodAppsState.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private val _dayHoursState = MutableStateFlow(DayHoursUiState())
    val dayHoursState: StateFlow<DayHoursUiState> = _dayHoursState.asStateFlow()

    private val _reelCountToday = MutableStateFlow(0)
    val reelCountToday: StateFlow<Int> = _reelCountToday.asStateFlow()

    private val _reelHistory = MutableStateFlow<List<DailyReelCount>>(emptyList())
    val reelHistory: StateFlow<List<DailyReelCount>> = _reelHistory.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            refreshToday(force)
        }
    }

    fun refreshReel(enabledApps: Set<String>) {
        enabledReelApps = enabledApps
        viewModelScope.launch { refreshReelNow() }
    }

    fun refreshHistory(force: Boolean = false, days: Int = 90) {
        viewModelScope.launch {
            refreshHistoryNow(force, days)
        }
    }

    suspend fun refreshAll() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        try {
            refreshToday(force = true)
            refreshHistoryNow(force = false)
            refreshReelNow()
        } finally {
            _isRefreshing.value = false
        }
    }

    fun openUsageAccessSettings() {
        repository.openUsageAccessSettings()
    }

    fun loadDayHours(date: LocalDate) {
        viewModelScope.launch {
            _dayHoursState.value = DayHoursUiState(date = date, isLoading = true)
            try {
                val hours = repository.getHourlyDeviceUsage(date)
                if (_dayHoursState.value.date != date) return@launch
                _dayHoursState.value = DayHoursUiState(
                    date = date,
                    isLoading = false,
                    hours = hours,
                    available = hours.isNotEmpty(),
                )
            } catch (_: Exception) {
                if (_dayHoursState.value.date != date) return@launch
                _dayHoursState.value = DayHoursUiState(date = date, isLoading = false)
            }
        }
    }

    fun loadMostUsed(date: LocalDate, limit: Int = 5) {
        viewModelScope.launch {
            _dayAppsState.value = _dayAppsState.value.copy(date = date, isLoading = true)
            try {
                val apps = topApps(date, date, limit)
                if (_dayAppsState.value.date != date) return@launch
                _dayAppsState.value = DayAppsUiState(date = date, apps = apps)
            } catch (_: Exception) {
                if (_dayAppsState.value.date == date) {
                    _dayAppsState.value = DayAppsUiState(date = date)
                }
            }
        }
    }

    fun loadMostUsedForPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 5,
    ) {
        viewModelScope.launch {
            _periodAppsState.value = PeriodAppsUiState(
                startDate = startDate,
                endDate = endDate,
                isLoading = true,
            )
            try {
                val apps = topApps(startDate, endDate, limit)
                if (!isCurrentPeriod(startDate, endDate)) return@launch
                _periodAppsState.value = PeriodAppsUiState(
                    startDate = startDate,
                    endDate = endDate,
                    apps = apps,
                )
            } catch (_: Exception) {
                if (isCurrentPeriod(startDate, endDate)) {
                    _periodAppsState.value = PeriodAppsUiState(
                        startDate = startDate,
                        endDate = endDate,
                    )
                }
            }
        }
    }

    fun loadAppDetails(packageName: String) {
        viewModelScope.launch {
            _appDetailsState.value = _appDetailsState.value.copy(
                isLoading = _appDetailsState.value.history.isEmpty(),
                error = null,
            )
            try {
                _appDetailsState.value = AppDetailsUiState(
                    isLoading = false,
                    history = repository.getAppUsageHistory(packageName, 7),
                )
            } catch (exception: Exception) {
                _appDetailsState.value = _appDetailsState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Unable to load app usage history.",
                )
            }
        }
    }

    private suspend fun refreshReelNow() {
        val enabledApps = enabledReelApps
        runCatching {
            _reelCountToday.value = reelRepository.todayTotal(enabledApps)
            _reelHistory.value = reelRepository.last7Days(enabledApps)
        }
    }

    private suspend fun refreshToday(force: Boolean) {
        val hasAccess = repository.hasUsageAccess()
        if (!hasAccess) {
            _uiState.value = UsageUiState(
                hasUsageAccess = false,
                error = "Usage access is required.",
            )
            return
        }
        val hasContent = _uiState.value.apps.isNotEmpty() ||
            _uiState.value.installedApps.isNotEmpty()
        _uiState.value = _uiState.value.copy(
            isLoading = !hasContent,
            hasUsageAccess = true,
            error = null,
        )
        try {
            val usage = repository.refreshUsage(force)
            val installed = repository.getInstalledApps()
            val usageDays = repository.getDailyDeviceUsage(days = 2)
            val today = LocalDate.now()
            val screenTime = usageDays
                .firstOrNull { it.date == today }
                ?.totalUsageMillis
                ?: 0L
            val yesterdayUsage = usageDays
                .firstOrNull { it.date == today.minusDays(1) }
                ?.totalUsageMillis
            val usageByPackage = usage.associateBy { it.packageName }
            val totalUsage = screenTime.coerceAtLeast(1L)
            val usedApps = usage
                .filter { it.totalTimeInForeground > 0L }
                .map { it.toAppUsage(totalUsage) }
                .sortedByDescending { it.totalUsageMillis }
            val allApps = installed
                .map { app ->
                    val appUsage = usageByPackage[app.packageName]
                    appUsage?.copy(appName = app.appName)?.toAppUsage(totalUsage)
                        ?: app.toUnusedApp()
                }
                .sortedWith(
                    compareByDescending<AppUsage> {
                        usageByPackage[it.packageName]?.totalTimeInForeground ?: 0L
                    }.thenBy { it.name.lowercase() },
                )
            _uiState.value = UsageUiState(
                isLoading = false,
                hasUsageAccess = true,
                apps = usedApps,
                installedApps = allApps,
                totalUsageMillis = screenTime,
                yesterdayUsageMillis = yesterdayUsage,
            )
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = exception.message ?: "Unable to load usage data.",
            )
        }
    }

    private suspend fun refreshHistoryNow(force: Boolean, days: Int = 90) {
        _historyState.value = _historyState.value.copy(
            isLoading = _historyState.value.days.isEmpty(),
            error = null,
        )
        try {
            _historyState.value = HistoryUiState(
                isLoading = false,
                days = repository.getDailyDeviceUsage(days, force),
            )
        } catch (exception: Exception) {
            _historyState.value = _historyState.value.copy(
                isLoading = false,
                error = exception.message ?: "Unable to load usage history.",
            )
        }
    }

    private suspend fun topApps(
        start: LocalDate,
        end: LocalDate,
        limit: Int,
    ): List<AppUsage> {
        val apps = repository.getTopAppsBetween(start, end)
        val total = apps.sumOf(AppUsageInfo::totalTimeInForeground).coerceAtLeast(1L)
        return apps
            .map { it.toAppUsage(total) }
            .sortedByDescending { it.totalUsageMillis }
            .take(limit)
    }

    private fun isCurrentPeriod(startDate: LocalDate, endDate: LocalDate): Boolean {
        return _periodAppsState.value.startDate == startDate &&
            _periodAppsState.value.endDate == endDate
    }
}
