package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.domain.UsagePeriod
import me.ankitgrai.kaizen.ui.design.SelectableChip
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

@Composable
fun PeriodSelector(
    selectedPeriod: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(18.dp),
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        UsagePeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            SelectableChip(
                label = period.label,
                selected = selected,
                onClick = { onPeriodSelected(period) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                fontSize = 11.sp,
                selectedFontWeight = FontWeight.SemiBold,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                fill = true,
                selectedContainer = MaterialTheme.colorScheme.primaryContainer,
                unselectedContainer = Color.Transparent,
                selectedContent = MaterialTheme.colorScheme.primary,
                unselectedContent = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun PeriodSelectorPreview() {
    KaizenTheme(darkTheme = true) {
        PeriodSelector(
            selectedPeriod = UsagePeriod.DAY,
            onPeriodSelected = {},
        )
    }
}
