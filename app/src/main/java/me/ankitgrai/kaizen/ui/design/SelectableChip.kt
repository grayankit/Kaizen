package me.ankitgrai.kaizen.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import me.ankitgrai.kaizen.ui.components.roundedClickable

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContainer: Color,
    unselectedContainer: Color,
    selectedContent: Color,
    unselectedContent: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    fontSize: TextUnit = TextUnit.Unspecified,
    selectedFontWeight: FontWeight = FontWeight.Normal,
    unselectedFontWeight: FontWeight = FontWeight.Normal,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    fill: Boolean = false,
) {
    Text(
        text = label,
        color = if (selected) selectedContent else unselectedContent,
        fontSize = fontSize,
        fontWeight = if (selected) selectedFontWeight else unselectedFontWeight,
        maxLines = 1,
        textAlign = if (fill) TextAlign.Center else TextAlign.Start,
        modifier = modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .roundedClickable(shape, onClick = onClick)
            .background(if (selected) selectedContainer else unselectedContainer)
            .padding(contentPadding),
    )
}
