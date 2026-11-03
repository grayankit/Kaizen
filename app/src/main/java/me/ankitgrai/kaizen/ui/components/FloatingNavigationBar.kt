package me.ankitgrai.kaizen.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

enum class CurrentScreen{
    Overview,
    Apps,
    History
}
@Composable
fun FloatingNavigationBar(
    selectedDestination: CurrentScreen,
    onScreenSelected: (CurrentScreen) -> Unit,
    modifier: Modifier = Modifier
){
    val activeColor = MaterialTheme.colorScheme.primaryContainer
    val iconColor = MaterialTheme.colorScheme.primary
    val barColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
    val indicatorOffset by animateDpAsState(
        targetValue = (IconItemWidth + ItemGap) * selectedDestination.ordinal,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "navIndicator"
    )

    Box(
        modifier = modifier
            .shadow(16.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(barColor)
            .padding(6.dp)
    ) {
    Box(
        modifier = Modifier
            .offset(x = indicatorOffset)
            .width(SelectedItemWidth)
            .height(ItemHeight)
            .clip(CircleShape)
            .background(activeColor)
    )
    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ItemGap)
    ){
        NavigationItem(
            label = "Overview",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Dashboard,
                    contentDescription = null
                )
            },
            selected = selectedDestination == CurrentScreen.Overview,
            iconColor = iconColor
        ) {
            onScreenSelected(CurrentScreen.Overview)
        }
        NavigationItem(
            label = "Apps",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Apps,
                    contentDescription = null
                )
            },
            selected = selectedDestination == CurrentScreen.Apps,
            iconColor = iconColor
        ) {
            onScreenSelected(CurrentScreen.Apps)
        }
        NavigationItem(
            label = "History",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null
                )
            },
            selected = selectedDestination == CurrentScreen.History,
            iconColor = iconColor
        ) {
            onScreenSelected(CurrentScreen.History)
        }
    }
    }
}

@Composable
private fun NavigationItem(
    icon: @Composable () -> Unit,
    selected: Boolean,
    iconColor: Color,
    modifier: Modifier  = Modifier,
    label: String? = null,
    onClick: () -> Unit
){
    val width by animateDpAsState(
        targetValue = if (selected && label != null) SelectedItemWidth else IconItemWidth,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "Navigation item width"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected && label != null) 1f else 0f,
        animationSpec = tween(180),
        label = "Navigation label"
    )
    Row(
        modifier = modifier
            .width(width)
            .height(ItemHeight)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(start = 13.dp, end = 13.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides iconColor
            ) {
                icon()
            }
        }
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .graphicsLayer { alpha = labelAlpha },
                color = iconColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private val ItemHeight = 56.dp
private val IconItemWidth = 56.dp
private val SelectedItemWidth = 156.dp
private val ItemGap = 4.dp

@Preview(showBackground = true)
@Composable
private fun FloatingNavigationBarPreview() {
    KaizenTheme {
        FloatingNavigationBar(
            selectedDestination = CurrentScreen.Overview,
            onScreenSelected = {}
        )
    }
}

