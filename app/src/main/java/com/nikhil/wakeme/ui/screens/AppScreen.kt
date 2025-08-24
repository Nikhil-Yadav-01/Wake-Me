package com.nikhil.wakeme.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.util.Resource

enum class ErrorMode { Inline, Toast }

@Composable
fun <T> AppScreen(
    resource: Resource<T>,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    errorMode: ErrorMode = ErrorMode.Toast,
    onError: ((String) -> Unit)? = null,
    onSuccess: @Composable (T) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when (resource) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Show a blurred view
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .blur(8.dp)
                            .alpha(0.7f)
                    )

                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            is Resource.Error -> {
                // trigger callback every error
                LaunchedEffect(resource.message) {
                    onError?.invoke(resource.message)
                }

                when (errorMode) {
                    ErrorMode.Inline -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = resource.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                onRetry?.let {
                                    Button(onClick = it) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                    ErrorMode.Toast -> {
                        // Show Toast once
                        LaunchedEffect(resource.message) {
                            Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is Resource.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "No alarms available",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is Resource.Success -> {
                onSuccess(resource.data)
            }
        }
    }
}
