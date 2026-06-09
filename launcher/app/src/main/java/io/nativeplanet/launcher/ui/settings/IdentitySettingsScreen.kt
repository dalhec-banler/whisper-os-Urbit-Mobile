package io.nativeplanet.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPButtonStyle
import io.nativeplanet.launcher.ui.components.SectionHeader
import io.nativeplanet.launcher.ui.components.SigilView
import io.nativeplanet.launcher.ui.components.StatusChip
import io.nativeplanet.launcher.ui.home.RuntimeStatusViewModel

@Composable
fun IdentitySettingsScreen(
    onBack: () -> Unit,
    onAddIdentity: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuntimeStatusViewModel = hiltViewModel()
) {
    val colors = NativePlanetTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val bootPackage = uiState.bootPackageStatus

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(NPSpacing.screenGutter)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "back",
                style = NPType.caption,
                color = colors.foregroundDim,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = NPSpacing.md)
            )

            Text(
                text = "Identity",
                style = NPType.displaySm,
                color = colors.foreground
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        SectionHeader(text = "Active Identity")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        // Active ship card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.backgroundSecondary)
                .padding(NPSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amber left border
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(80.dp)
                    .background(colors.accentSoft)
            )

            Spacer(modifier = Modifier.width(NPSpacing.md))

            SigilView(
                patp = bootPackage.ship ?: "~zod",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(NPSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bootPackage.ship ?: "no ship",
                    style = NPType.patpLg,
                    color = colors.foreground
                )

                Spacer(modifier = Modifier.height(NPSpacing.xs))

                bootPackage.parent?.let { parent ->
                    Text(
                        text = "mobile moon · under $parent",
                        style = NPType.bodySm,
                        color = colors.foregroundDim
                    )
                }

                Spacer(modifier = Modifier.height(NPSpacing.sm))

                StatusChip(state = uiState.runtimeStatus.state)
            }
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        SectionHeader(text = "Details")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.backgroundSecondary)
                .padding(NPSpacing.cardPadding)
        ) {
            DetailRow("Boot Mode", bootPackage.bootMode?.name ?: "—")
            DetailRow("Pier Path", bootPackage.pierPath ?: "—")
            DetailRow("Pill Path", bootPackage.pillPath ?: "—")
            DetailRow("Pier Exists", if (bootPackage.pierExists) "yes" else "no")
            DetailRow("Key File", if (bootPackage.keyFileExists) "present" else "missing")

            if (bootPackage.validationErrors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(NPSpacing.md))
                Text(
                    text = "Validation Errors:",
                    style = NPType.bodySm,
                    color = colors.error
                )
                bootPackage.validationErrors.forEach { error ->
                    Text(
                        text = "• ${error.field}: ${error.message}",
                        style = NPType.nano,
                        color = colors.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        SectionHeader(text = "Other Identities")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.backgroundSecondary)
                .padding(NPSpacing.cardPadding)
        ) {
            Text(
                text = "Only this moon is on the phone right now.",
                style = NPType.bodySm,
                color = colors.foregroundDim
            )

            Spacer(modifier = Modifier.height(NPSpacing.xs))

            Text(
                text = "Add another identity when you want a second mobile moon or a recovery import.",
                style = NPType.nano,
                color = colors.foregroundFaint
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        NPButton(
            text = "Add identity",
            onClick = onAddIdentity,
            style = NPButtonStyle.SECONDARY
        )

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        Text(
            text = "Pair another moon or use a moon key.",
            style = NPType.caption,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NPSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = NPType.bodySm,
            color = colors.foregroundDim
        )
        Text(
            text = value,
            style = NPType.caption,
            color = colors.foreground,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}
