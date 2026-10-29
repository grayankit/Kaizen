package me.ankitgrai.kaizen.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.usage.AppUsageInfo
import me.ankitgrai.kaizen.data.usage.DailyAppUsage
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.data.usage.InstalledAppInfo
import me.ankitgrai.kaizen.data.usage.UsageRepository
import me.ankitgrai.kaizen.ui.components.AppUsage
import java.time.LocalDate

data class UsageUiState(
    val isLoading: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val apps: List<AppUsage> = emptyList(),
    val installedApps: List<AppUsage> = emptyList(),
    val totalUsageMills: Long = 0L,
    val error: String? = null
)
data class AppDetailsUiState(
    val isLoading: Boolean = false,
    val history: List<DailyAppUsage> = emptyList(),
    val error: String? = null
)

data class DayAppsUiState(
    val date: LocalDate? = null,
    val isLoading: Boolean = false,
    val apps: List<AppUsage> = emptyList()
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val days: List<DailyDeviceUsage> = emptyList(),
    val error: String? = null
)
class UsageViewModel(
    application: Application
): AndroidViewModel(application){
    private val  repository = UsageRepository(application)

    private val _uiState  = MutableStateFlow(UsageUiState())

    private val _appDetailsState = MutableStateFlow(AppDetailsUiState())

    private val _dayAppsState = MutableStateFlow(DayAppsUiState())
    val dayAppsState: StateFlow<DayAppsUiState> = _dayAppsState.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryUiState())

    val historyState: StateFlow<HistoryUiState> =
        _historyState.asStateFlow()
    val appDetailsState: StateFlow<AppDetailsUiState> =
        _appDetailsState.asStateFlow()
    val uiState: StateFlow<UsageUiState>  = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            refreshToday(force)
        }
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
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun refreshToday(force: Boolean) {
            val hasAccess  = repository.hasUsageAccess()
            if(!hasAccess){
                _uiState.value = UsageUiState(
                    hasUsageAccess = false,
                    error = "Usage access is required."
                )
                return
            }
            val hasContent = _uiState.value.apps.isNotEmpty() ||
                _uiState.value.installedApps.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasContent,
                hasUsageAccess = true,
                error = null
            )
            try {
                val usage = repository.refreshUsage(force)
                val installed = repository.getInstalledApps()

                val usageByPackage = usage.associateBy {
                    it.packageName
                }

                val totalUsage = usage.sumOf {
                    it.totalTimeInForeground
                }

                val usedApps = usage
                    .filter { it.totalTimeInForeground > 0L}
                    .map { it.toAppUsage(totalUsage) }
                    .sortedByDescending {
                        it.totalUsageMillis
                    }
                val allApps = installed
                    .map { app->
                        val appUsage  =usageByPackage[app.packageName]

                        appUsage?.copy(appName = app.appName)?.toAppUsage(totalUsage) ?: app.toUnusedApp()
                    }
                    .sortedWith(
                        compareByDescending < AppUsage>{
                            usageByPackage[it.packageName]?.totalTimeInForeground ?: 0L
                        }.thenBy {
                            it.name.lowercase()
                        }
                    )
                _uiState.value = UsageUiState(
                    isLoading = false,
                    hasUsageAccess = true,
                    apps = usedApps,
                    installedApps = allApps,
                    totalUsageMills = usage.sumOf {
                        it.totalTimeInForeground
                    }
                )
            } catch (exception: Exception){
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message?:"Unable to load usage data."
                )
            }
    }

    private suspend fun refreshHistoryNow(force: Boolean, days: Int = 90) {
            _historyState.value = _historyState.value.copy(
                isLoading = _historyState.value.days.isEmpty(),
                error = null
            )

            try {
                _historyState.value = HistoryUiState(
                    isLoading = false,
                    days = repository.getDailyDeviceUsage(days, force)
                )
            } catch (exception: Exception) {
                _historyState.value = _historyState.value.copy(
                    isLoading = false,
                    error = exception.message
                        ?: "Unable to load usage history."
                )
            }
    }
    private fun AppUsageInfo.toAppUsage(totalDailyUsage: Long): AppUsage{
        val colors = listOf(
            Color(0xFFFF8178),
            Color(0xFFC49BFF),
            Color(0xFF8CB8FF),
            Color(0xFFA6D989),
            Color(0xFFFFC078)
        )

        val initials = appName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

        val progress = if (totalDailyUsage > 0L) {
            totalTimeInForeground.toFloat() / totalDailyUsage
        } else {
            0f
        }

        return AppUsage(
            name = appName,
            category = categorizedApp(packageName,appName),
            duration = formatDuration(totalTimeInForeground),
            initials  = initials.ifBlank { "?" },
            color = colors[(packageName.hashCode() and Int.MAX_VALUE) % colors.size],
            usageProgress = progress.coerceIn(0f,1f),
            packageName = packageName,
            totalUsageMillis = totalTimeInForeground
        )
    }
    private fun InstalledAppInfo.toUnusedApp(): AppUsage{
        val colors = listOf(
            Color(0xFFFF8178),
            Color(0xFFC49BFF),
            Color(0xFF8CB8FF),
            Color(0xFFA6D989),
            Color(0xFFFFC078)
        )

        val initials = appName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString ("") {
                it.first().uppercase()
            }
        return AppUsage(
            name = appName,
            category = categorizedApp(packageName,appName),
            duration = "0m",
            initials = initials.ifBlank { "?" },
            color = colors[
                (packageName.hashCode() and Int.MAX_VALUE) % colors.size
            ],
            usageProgress = 0f,
            packageName = packageName
        )
    }
    private fun formatDuration(milliseconds: Long): String{
        val totalMinutes = milliseconds/60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if(hours > 0){
            "${hours}h ${minutes}m"
        }else{
            "${minutes}m"
        }
    }
    fun openUsageAccessSettings(){
        repository.openUsageAccessSettings()
    }

    fun loadMostUsed(date: LocalDate, limit: Int = 5) {
        viewModelScope.launch {
            _dayAppsState.value = _dayAppsState.value.copy(
                date = date,
                isLoading = true
            )
            try {
                val apps = repository.getTopAppsForDate(date)
                val total = apps.sumOf { it.totalTimeInForeground }.coerceAtLeast(1L)
                _dayAppsState.value = DayAppsUiState(
                    date = date,
                    apps = apps
                        .map { it.toAppUsage(total) }
                        .sortedByDescending { it.totalUsageMillis }
                        .take(limit)
                )
            } catch (exception: Exception) {
                _dayAppsState.value = DayAppsUiState(date = date)
            }
        }
    }

    fun loadAppDetails(packageName: String){
        viewModelScope.launch {
            _appDetailsState.value = _appDetailsState.value.copy(
                isLoading = _appDetailsState.value.history.isEmpty(),
                error = null
            )

            try {
                val history = repository.getAppUsageHistory(
                    packageName,
                    7
                )
                _appDetailsState.value = AppDetailsUiState(
                    false,
                    history
                )
            } catch (exception: Exception) {
                _appDetailsState.value = _appDetailsState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Unable to load app usage history."
                )
            }
        }
    }
    private fun categorizedApp(
        packageName: String,
        appName: String
    ): String{
        val id = "$packageName $appName".lowercase()

        return when {
            listOf(
                "instagram", "discord", "whatsapp", "telegram",
                "facebook", "messenger", "snapchat", "reddit",
                "signal", "threads", "x.android"
            ).any { it in id } -> "Social"

            listOf(
                "youtube", "netflix", "twitch", "primevideo",
                "hotstar", "video", "vlc", "mxplayer",
                "crunchyroll", "morphe"
            ).any { it in id } -> "Video"

            listOf(
                "spotify", "music", "soundcloud", "deezer",
                "gaana", "jiosaavn", "podcast"
            ).any { it in id } -> "Music"

            listOf(
                "genshin", "minecraft", "roblox", "game",
                "pubg", "freefire", "clashofclans",
                "playstation", "xbox"
            ).any { it in id } -> "Games"

            listOf(
                "vscode", "visual.studio", "notion",
                "office", "docs", "sheets", "slack",
                "github", "gitlab", "trello", "evernote"
            ).any { it in id } -> "Work"

            else -> "Other"
        }
    }

}