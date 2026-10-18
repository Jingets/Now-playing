package com.sofamaven.musicrecognizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mandatory copyright watermark placed on all screens in the bottom-right corner.
 * Required text: "© Sofa Maven SDE / КРПО Диванный умник"
 */
@Composable
fun BoxScope.CopyrightWatermark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 32.dp, bottom = 24.dp)
            .background(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = "© Sofa Maven SDE / КРПО Диванный умник",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}
