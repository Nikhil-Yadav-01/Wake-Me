package com.nikhil.wakeme.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.util.gradientBrush

// --- Gradient Section Card ---
@Composable
fun GradientSectionCard(
    colors: List<Color>, title: String? = null, content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, gradientBrush(colors), RoundedCornerShape(20.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Companion.Transparent)
    ) {
        Column(
            modifier = Modifier.Companion.padding(16.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.Companion.height(12.dp))
            }
            content()
        }
    }
}