package me.ankitgrai.kaizen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
    val borderColor = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.primaryContainer
    val iconColor = MaterialTheme.colorScheme.primary

    Row (
        modifier = modifier
            .border(
                width = 2.dp,
                color = borderColor,
                shape = CircleShape
            )
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            activeColor = activeColor,
            iconColor  = iconColor,
            modifier  = Modifier.weight(1f)
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
            activeColor = activeColor,
            iconColor = iconColor,
            modifier = Modifier.weight(1f)
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
            activeColor = activeColor,
            iconColor = iconColor,
            modifier  = Modifier.weight(1f)
        ) {
            onScreenSelected(CurrentScreen.History)
        }
    }
}

@Composable
private fun NavigationItem(
    icon: @Composable () -> Unit,
    selected: Boolean,
    activeColor: Color,
    iconColor: Color,
    modifier: Modifier  = Modifier,
    label: String? = null,
    onClick: () -> Unit
){
    val backgroundColor by animateColorAsState(
        targetValue = if(selected) activeColor else Color.Transparent,
        animationSpec = tween(250),
        label = "Navigation background"
    )
    val height by animateDpAsState(
        targetValue = 60.dp,
        tween(250),
        "Navigation item height"
    )
    Row(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if(selected && label != null) 18.dp else 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ){
            CompositionLocalProvider(
                LocalContentColor provides iconColor
            ) {
                icon()
            }
        }
        if(selected && label != null){
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                color = iconColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

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

