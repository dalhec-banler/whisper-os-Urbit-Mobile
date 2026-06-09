package io.nativeplanet.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.RuntimeState
import io.nativeplanet.launcher.domain.model.RuntimeStatus
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.SectionHeader
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RuntimeDetailsPanel(
    runtimeStatus: RuntimeStatus,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundSecondary)
            .padding(NPSpacing.cardPadding)
    ) {
        SectionHeader(text = "Runtime")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        DetailRow(
            label = "State",
            value = runtimeStatus.state.displayName(),
            valueColor = runtimeStatus.state.color()
        )

        runtimeStatus.shipName?.let { ship ->
            DetailRow(label = "Ship", value = ship)
        }

        runtimeStatus.version?.let { version ->
            DetailRow(label = "Vere", value = version)
        }

        runtimeStatus.bootMode?.let { mode ->
            DetailRow(label = "Boot Mode", value = mode.name)
        }

        runtimeStatus.pid?.let { pid ->
            DetailRow(label = "PID", value = pid.toString())
        }

        runtimeStatus.uptimeMs?.let { uptime ->
            DetailRow(label = "Uptime", value = formatUptime(uptime))
        }

        runtimeStatus.lastStartTime?.let { time ->
            DetailRow(label = "Started", value = formatTimestamp(time))
        }

        runtimeStatus.lastStopTime?.let { time ->
            DetailRow(label = "Stopped", value = formatTimestamp(time))
        }

        runtimeStatus.lastSuccessfulPoll?.let { pollMs ->
            DetailRow(label = "Last Poll", value = formatEpochMillis(pollMs))
        }

        DetailRow(
            label = "conn.sock",
            value = if (runtimeStatus.connSockAvailable) "available" else "unavailable",
            valueColor = if (runtimeStatus.connSockAvailable) NPColors.accentSage else colors.foregroundDim
        )

        runtimeStatus.exitCode?.let { code ->
            DetailRow(
                label = "Exit Code",
                value = code.toString(),
                valueColor = if (code != 0) colors.error else colors.foreground
            )
        }

        runtimeStatus.lastError?.let { error ->
            Spacer(modifier = Modifier.height(NPSpacing.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.error.copy(alpha = 0.1f))
                    .padding(NPSpacing.sm)
            ) {
                Column {
                    Text(
                        text = error.code,
                        style = NPType.caption,
                        color = colors.error
                    )
                    Text(
                        text = error.message,
                        style = NPType.nano,
                        color = colors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = NativePlanetTheme.colors.foreground
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NPSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = NPType.bodySm,
            color = NativePlanetTheme.colors.foregroundDim
        )
        Text(
            text = value,
            style = NPType.caption,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 210.dp)
        )
    }
}

private fun RuntimeState.displayName(): String = when (this) {
    RuntimeState.RUNNING -> "Running"
    RuntimeState.STOPPED -> "Stopped"
    RuntimeState.STARTING -> "Starting..."
    RuntimeState.STOPPING -> "Stopping..."
    RuntimeState.ERROR -> "Error"
    RuntimeState.CRASHED -> "Crashed"
    RuntimeState.UNINITIALIZED -> "Not Configured"
}

@Composable
private fun RuntimeState.color(): androidx.compose.ui.graphics.Color {
    val colors = NativePlanetTheme.colors
    return when (this) {
        RuntimeState.RUNNING -> NPColors.accentSage
        RuntimeState.STOPPED -> colors.foregroundDim
        RuntimeState.STARTING, RuntimeState.STOPPING -> NPColors.accentAmber
        RuntimeState.ERROR, RuntimeState.CRASHED -> colors.error
        RuntimeState.UNINITIALIZED -> colors.foregroundFaint
    }
}

private fun formatUptime(uptimeMs: Long): String {
    val duration = Duration.ofMillis(uptimeMs)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatTimestamp(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun formatEpochMillis(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMs))
}
