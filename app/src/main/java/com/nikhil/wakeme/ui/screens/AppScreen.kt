package com.nikhil.wakeme.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.util.Resource

enum class ErrorMode { Inline, Toast }

@Composable
fun <T> AppScreen(
    resource: Resource<T>,
    modifier: Modifier = Modifier,
    backgroundResId: Int,
    onRetry: (() -> Unit)? = null,
    errorMode: ErrorMode = ErrorMode.Inline,
    onError: ((String) -> Unit)? = null,
    content: @Composable (T) -> Unit
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
                    CircularProgressIndicator()
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
                content(resource.data)
            }
        }
    }
}
