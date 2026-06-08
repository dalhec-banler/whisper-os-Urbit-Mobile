package io.nativeplanet.launcher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.DiagnosticsSummary
import io.nativeplanet.launcher.domain.model.LogLevel
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.SectionHeader
import java.time.format.DateTimeFormatter

@Composable
fun DiagnosticsPanel(
    diagnostics: DiagnosticsSummary,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    var showResolver by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundSecondary)
            .padding(NPSpacing.cardPadding)
    ) {
        SectionHeader(text = "Diagnostics")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        // Recent errors
        if (diagnostics.recentErrors.isNotEmpty()) {
            Text(
                text = "Recent Errors (${diagnostics.recentErrors.size})",
                style = NPType.bodySm,
                color = colors.error
            )
            Spacer(modifier = Modifier.height(NPSpacing.xs))
            diagnostics.recentErrors.take(3).forEach { error ->
                Text(
                    text = "[${error.source}] ${error.message}",
                    style = NPType.nano,
                    color = colors.error,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(NPSpacing.md))
        }

        // Resolver contents (collapsible)
        Text(
            text = if (showResolver) "▼ Resolver Contents" else "▶ Resolver Contents",
            style = NPType.bodySm,
            color = colors.foregroundDim,
            modifier = Modifier
                .clickable { showResolver = !showResolver }
                .padding(vertical = NPSpacing.xs)
        )

        AnimatedVisibility(visible = showResolver) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.background)
                    .padding(NPSpacing.sm)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = diagnostics.resolverContents ?: "(empty)",
                    style = NPType.nano,
                    color = colors.foregroundDim
                )
            }
        }

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        // Logs (collapsible)
        Text(
            text = if (showLogs) "▼ Controller Logs" else "▶ Controller Logs",
            style = NPType.bodySm,
            color = colors.foregroundDim,
            modifier = Modifier
                .clickable { showLogs = !showLogs }
                .padding(vertical = NPSpacing.xs)
        )

        AnimatedVisibility(visible = showLogs) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.background)
                    .padding(NPSpacing.sm)
                    .verticalScroll(rememberScrollState())
            ) {
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                diagnostics.controllerLogs.forEach { log ->
                    val time = log.timestamp.atZone(java.time.ZoneId.systemDefault()).format(formatter)
                    val levelColor = when (log.level) {
                        LogLevel.ERROR -> colors.error
                        LogLevel.WARN -> NPColors.accentAmber
                        LogLevel.INFO -> colors.foregroundDim
                    }
                    Text(
                        text = "$time ${log.level.name} ${log.message}",
                        style = NPType.nano,
                        color = levelColor,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
