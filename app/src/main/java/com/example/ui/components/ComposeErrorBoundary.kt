package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Local Composition provider to allow nested components to throw errors programmatically
 * to the nearest ancestor Error Boundary (similar to throwing during React render or event loops).
 */
val LocalErrorBoundary = staticCompositionLocalOf<((Throwable) -> Unit)?> { null }

/**
 * ComposeErrorBoundary acts as a React-style Error Boundary wrapper for Jetpack Compose components.
 * It catches runtime exceptions during composition and offers local recovery logic and beautiful
 * custom telemetry/logging.
 */
@Composable
fun ComposeErrorBoundary(
    componentName: String,
    onReset: (() -> Unit)? = null,
    fallback: @Composable (Throwable, () -> Unit) -> Unit = { error, reset ->
        DefaultErrorFallback(componentName, error, reset)
    },
    content: @Composable () -> Unit
) {
    var errorState by remember { mutableStateOf<Throwable?>(null) }

    // Clear error state and invoke onReset callbacks securely
    val resetAction: () -> Unit = {
        errorState = null
        onReset?.invoke()
    }

    if (errorState != null) {
        fallback(errorState!!, resetAction)
    } else {
        // Enforce CompositionLocalProvider to propagate standard crash/error logs downstream
        CompositionLocalProvider(
            LocalErrorBoundary provides { exception ->
                Log.e("ComposeErrorBoundary", "Custom async exception caught in $componentName boundary", exception)
                errorState = exception
            }
        ) {
            content()
        }
    }
}

/**
 * Highly elegant, luxury dark-mode aligned fallback UI when an crash/error occurs.
 */
@Composable
fun DefaultErrorFallback(
    componentName: String,
    error: Throwable,
    onReset: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    val stackTrace = remember(error) {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        writer.toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E11)) // Luxury dark foundation
            .padding(24.dp)
            .testTag("error_boundary_fallback_$componentName"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Distinct Alert Glow Symbol
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                    .border(2.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert icon",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Heading Styled meticulously with high contrast hierarchy
            Text(
                text = "Something went wrong",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "An unexpected error occurred in the $componentName.",
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button: Recover from failure
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("error_boundary_reset_$componentName"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset module",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Refresh Component",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic button helper. Ensures 48dp touch targets.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { showDetails = !showDetails }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics icon",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showDetails) "Hide Diagnostics" else "Show Diagnostics",
                        fontSize = 12.sp,
                        color = Color(0xFFC084FC),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (showDetails) "▲" else "▼",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            if (showDetails) {
                Spacer(modifier = Modifier.height(12.dp))
                // Console-styled visual code log container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "EXCEPTION TYPE: ${error.javaClass.simpleName}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error.localizedMessage ?: "No supplementary error details.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stackTrace,
                            color = Color.Gray.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
