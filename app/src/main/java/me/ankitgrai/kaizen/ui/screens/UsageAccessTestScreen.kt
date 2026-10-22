package me.ankitgrai.kaizen.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.usage.AppUsageInfo
import me.ankitgrai.kaizen.data.usage.UsageRepository

@Composable
fun UsageAccessTestScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember {
        UsageRepository(context)
    }
    val scope = rememberCoroutineScope()

    var hasAccess by remember {
        mutableStateOf(repository.hasUsageAccess())
    }

    var apps by remember {
        mutableStateOf<List<AppUsageInfo>>(emptyList())
    }

    var message by remember {
        mutableStateOf("Grant usage access to begin.")
    }

    fun refreshUsage() {
        hasAccess = repository.hasUsageAccess()

        if (!hasAccess) {
            message = "Usage access has not been granted."
            apps = emptyList()
            return
        }

        scope.launch {
            message = "Loading usage data..."

            try {
                apps = repository.getTodayUsage()
                message = if (apps.isEmpty()) {
                    "No usage records found for today."
                } else {
                    "Today's application usage"
                }
            } catch (exception: Exception) {
                message = "Unable to load usage data: ${exception.message}"
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshUsage()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Usage Access Test",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = if (hasAccess) {
                "Usage access granted"
            } else {
                "Usage access not granted"
            }
        )

        Button(
            onClick = {
                repository.openUsageAccessSettings()
            }
        ) {
            Text("Grant Usage Access")
        }

        Button(
            onClick = {
                refreshUsage()
            }
        ) {
            Text("Refresh Usage")
        }

        Text(message)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = apps,
                key = { it.packageName }
            ) { app ->
                val minutes =
                    app.totalTimeInForeground / 60_000L

                val hours = minutes / 60
                val remainingMinutes = minutes % 60

                Text(
                    text = "${app.appName} — ${hours}h ${remainingMinutes}m"
                )
            }
        }
    }
}