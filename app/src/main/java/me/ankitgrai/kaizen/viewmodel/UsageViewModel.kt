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
import me.ankitgrai.kaizen.data.usage.UsageRepository
import me.ankitgrai.kaizen.ui.components.AppUsage

data class UsageUiState(
    val isLoading: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val apps: List<AppUsage> = emptyList(),
    val totalUsageMills: Long = 0L,
    val error: String? = null
)
class UsageViewModel(
    application: Application
): AndroidViewModel(application){
    private val  repository = UsageRepository(application)

    private val _uiState  = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState>  = _uiState.asStateFlow()

    fun refresh(){
        viewModelScope.launch {
            val hasAccess  = repository.hasUsageAccess()
            if(!hasAccess){
                _uiState.value = UsageUiState(
                    hasUsageAccess = false,
                    error = "Usage access is required."
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                hasUsageAccess = true,
                error = null
            )
            try {
                val usage   = repository.getTodayUsage()
                val maxUsage  = usage.maxOfOrNull {
                    it.totalTimeInForeground
                }?:1L
                _uiState.value = UsageUiState(
                    hasUsageAccess = true,
                    apps = usage.map{it.toAppUsage(maxUsage)},
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
    }
    private fun AppUsageInfo.toAppUsage(maxUsage: Long): AppUsage{
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

        val minutes = totalTimeInForeground / 60_000L
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        val duration = when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            else -> "${minutes}m"
        }

        return AppUsage(
            name = appName,
            category = "Application",
            duration = duration,
            initials  = initials.ifBlank { "?" },
            color = colors[(packageName.hashCode() and Int.MAX_VALUE) % colors.size],
            usageProgress = (
                    totalTimeInForeground.toFloat() /maxUsage.coerceAtLeast(1L)
                    ).coerceIn(0f,1f),
            packageName = packageName
        )
    }

    fun openUsageAccessSettings(){
        repository.openUsageAccessSettings()
    }
}