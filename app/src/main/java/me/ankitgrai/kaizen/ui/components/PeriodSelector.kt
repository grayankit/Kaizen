package me.ankitgrai.kaizen.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

enum class UsagePeriod {
    DAY,
    WEEK,
    MONTH
}

@Composable
fun PeriodSelector(
    selectedPeriod: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Color(0xFF171923),
                RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        UsagePeriod.entries.forEach { period ->
            val selected = period == selectedPeriod

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected) {
                            Color(0xFF203F42)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        onPeriodSelected(period)
                    }
                    .padding(
                        horizontal = 12.dp,
                        vertical = 9.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    color = if (selected) {
                        Color(0xFF9DD5D9)
                    } else {
                        Color(0xFFA5A7B8)
                    },
                    fontSize = 11.sp,
                    fontWeight = if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun PeriodSelectorPreview() {
    KaizenTheme(darkTheme = true) {
        PeriodSelector(
            selectedPeriod = UsagePeriod.DAY,
            onPeriodSelected = {}
        )
    }
}

