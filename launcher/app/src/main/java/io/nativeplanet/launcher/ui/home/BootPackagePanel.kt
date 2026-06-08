package io.nativeplanet.launcher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.BootPackageStatus
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.SectionHeader

@Composable
fun BootPackagePanel(
    bootPackageStatus: BootPackageStatus,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors
    var showDetails by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundSecondary)
            .padding(NPSpacing.cardPadding)
    ) {
        SectionHeader(text = "Boot Package")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        if (!bootPackageStatus.exists) {
            Text(
                text = "No boot package configured",
                style = NPType.bodySm,
                color = colors.foregroundDim
            )
            Text(
                text = "Complete onboarding to configure your ship",
                style = NPType.nano,
                color = colors.foregroundFaint
            )
        } else {
            PackageRow(
                label = "Status",
                value = if (bootPackageStatus.valid) "Valid" else "Invalid",
                valueColor = if (bootPackageStatus.valid) NPColors.accentSage else colors.error
            )

            bootPackageStatus.bootMode?.let { mode ->
                PackageRow(label = "Type", value = mode.name)
            }

            bootPackageStatus.packageVersion?.let { version ->
                PackageRow(label = "Version", value = "v$version")
            }

            Spacer(modifier = Modifier.height(NPSpacing.sm))

            // File status indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NPSpacing.md)
            ) {
                FileIndicator(
                    label = "Pier",
                    exists = bootPackageStatus.pierExists,
                    modifier = Modifier.weight(1f)
                )
                FileIndicator(
                    label = "Pill",
                    exists = bootPackageStatus.pillExists,
                    modifier = Modifier.weight(1f)
                )
                FileIndicator(
                    label = "Key",
                    exists = bootPackageStatus.keyFileExists,
                    modifier = Modifier.weight(1f)
                )
            }

            // Show validation errors if any
            if (bootPackageStatus.validationErrors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(NPSpacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.error.copy(alpha = 0.1f))
                        .padding(NPSpacing.sm)
                ) {
                    Column {
                        Text(
                            text = "Validation Errors",
                            style = NPType.caption,
                            color = colors.error
                        )
                        bootPackageStatus.validationErrors.forEach { error ->
                            Text(
                                text = "${error.field}: ${error.message}",
                                style = NPType.nano,
                                color = colors.error
                            )
                        }
                    }
                }
            }

            // Expandable paths section
            Spacer(modifier = Modifier.height(NPSpacing.sm))
            Text(
                text = if (showDetails) "▼ Paths" else "▶ Paths",
                style = NPType.nano,
                color = colors.foregroundFaint,
                modifier = Modifier
                    .clickable { showDetails = !showDetails }
                    .padding(vertical = NPSpacing.xs)
            )

            AnimatedVisibility(visible = showDetails) {
                Column {
                    bootPackageStatus.pierPath?.let { path ->
                        PathRow(label = "Pier", path = path)
                    }
                    bootPackageStatus.pillPath?.let { path ->
                        PathRow(label = "Pill", path = path)
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageRow(
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
            color = valueColor
        )
    }
}

@Composable
private fun FileIndicator(
    label: String,
    exists: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (exists) NPColors.accentSage.copy(alpha = 0.1f)
                else colors.error.copy(alpha = 0.1f)
            )
            .padding(NPSpacing.sm)
    ) {
        Column {
            Text(
                text = label,
                style = NPType.nano,
                color = colors.foregroundDim
            )
            Text(
                text = if (exists) "✓" else "✗",
                style = NPType.caption,
                color = if (exists) NPColors.accentSage else colors.error
            )
        }
    }
}

@Composable
private fun PathRow(
    label: String,
    path: String,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Column(modifier = modifier.padding(vertical = NPSpacing.xs)) {
        Text(
            text = label,
            style = NPType.nano,
            color = colors.foregroundFaint
        )
        Text(
            text = path,
            style = NPType.nano,
            color = colors.foregroundDim
        )
    }
}
