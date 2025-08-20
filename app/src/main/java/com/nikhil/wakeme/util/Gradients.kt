package com.nikhil.wakeme.util

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Gradient util
fun gradientBrush(colors: List<Color>) = Brush.Companion.linearGradient(colors)

val gradients = listOf(Color(0xFFFFC371), Color(0xFF9D00FF))