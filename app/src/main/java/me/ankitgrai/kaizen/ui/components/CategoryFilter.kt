package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

val APP_CATEGORIES = listOf(
    "All",
    "Social",
    "Video",
    "Work",
    "Music"
)

@Composable
fun CategoryFilter(
    selectedCategory: String,
    onCategorySelected:(String) -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ){
        APP_CATEGORIES.forEach { category ->
            val selected = category == selectedCategory
            Box(
              modifier = Modifier
                  .weight(1f)
                  .height(32.dp)
                  .background(
                      color = if (selected) {
                          Color(0xFF203F42)
                      } else {
                          Color(0xFF171923)
                      },
                      shape = RoundedCornerShape(50)
                  )
                  .clickable{
                      onCategorySelected(category)
                  },
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = category,
                    color = if (selected) {
                        Color(0xFF9DD5D9)
                    } else {
                        Color(0xFFA5A7B8)
                    },
                    fontSize = 10.sp,
                    fontWeight = if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    maxLines = 1
                )
            }
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
