package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.domain.AppCategory
import me.ankitgrai.kaizen.ui.design.SelectableChip

val APP_CATEGORIES = AppCategory.labels

@Composable
fun CategoryFilter(
    selectedCategory: String,
    onCategorySelected:(String) -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ){
        APP_CATEGORIES.forEach { category ->
            val selected = category == selectedCategory
            SelectableChip(
                label = category,
                selected = selected,
                onClick = { onCategorySelected(category) },
                selectedContainer = MaterialTheme.colorScheme.primaryContainer,
                unselectedContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
                selectedContent = MaterialTheme.colorScheme.primary,
                unselectedContent = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                selectedFontWeight = FontWeight.SemiBold,
            )
        }
    }
}
@Preview
@Composable
private fun CategoryFilterPreview(){
    CategoryFilter(
        selectedCategory = "All",
        onCategorySelected = {}
    )
}
