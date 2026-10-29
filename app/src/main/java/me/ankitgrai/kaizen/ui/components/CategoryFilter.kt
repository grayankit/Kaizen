package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val APP_CATEGORIES = listOf(
    "All",
    "Social",
    "Video",
    "Work",
    "Music",
    "Games",
    "Other"
)

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
            Text(
                text = category,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp,
                fontWeight = if(selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                    .clickable {
                        onCategorySelected(category)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
