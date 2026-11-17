package me.ankitgrai.kaizen.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets

val TabScreenPadding = PaddingValues(
    start = 20.dp,
    end = 20.dp,
    top = 24.dp,
    bottom = 120.dp,
)

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    useDefaultColors: Boolean = false,
    tintContent: Boolean = true,
) {
    TopAppBar(
        windowInsets = WindowInsets(0.dp),
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        colors = when {
            useDefaultColors -> TopAppBarDefaults.topAppBarColors()
            tintContent -> TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = contentColor,
                navigationIconContentColor = contentColor,
            )
            else -> TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
        },
    )
}

@Composable
fun CenteredProgress(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    indicatorSize: Dp? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = if (indicatorSize != null) Modifier.size(indicatorSize) else Modifier,
        )
    }
}

@Composable
fun PromptMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
