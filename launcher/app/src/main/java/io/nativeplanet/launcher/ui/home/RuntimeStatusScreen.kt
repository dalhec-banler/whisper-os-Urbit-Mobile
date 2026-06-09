package io.nativeplanet.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.nativeplanet.launcher.domain.IdentityMode
import io.nativeplanet.launcher.domain.model.RuntimeState
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPButtonStyle
import io.nativeplanet.launcher.ui.components.SigilView
import io.nativeplanet.launcher.ui.components.StatusChip

@Composable
fun RuntimeStatusScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuntimeStatusViewModel = hiltViewModel()
) {
    val colors = NativePlanetTheme.colors
    val uiState by viewModel.uiState.collectAsState()

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "NativePlanet",
                style = NPType.displaySm,
                color = colors.foreground
            )

            Text(
                text = "⚙",
                style = NPType.displaySm,
                color = colors.foregroundDim,
                modifier = Modifier
                    .clickable(onClick = onNavigateToSettings)
                    .padding(NPSpacing.sm)
            )
        }

        // Demo data banner
        if (uiState.usingDemoData) {
            Spacer(modifier = Modifier.height(NPSpacing.md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(NPColors.accentAmber.copy(alpha = 0.15f))
                    .padding(NPSpacing.md)
            ) {
                Text(
                    text = if (uiState.controllerAvailable)
                        "Controller connected"
                    else
                        "Controller unavailable · using demo data",
                    style = NPType.caption,
                    color = NPColors.accentAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        // Ship card - use demo identity if configured, otherwise fall back to boot package
        val displayShip = uiState.demoIdentity.shipName ?: uiState.bootPackageStatus.ship
        val displayParent = uiState.demoIdentity.parentName ?: uiState.bootPackageStatus.parent
        val isConfigured = uiState.demoIdentity.isConfigured || uiState.bootPackageStatus.exists

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.backgroundSecondary)
                .padding(NPSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SigilView(
                patp = displayShip ?: "~zod",
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(NPSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayShip ?: "no ship configured",
                    style = NPType.patp,
                    color = colors.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                when {
                    uiState.demoIdentity.mode == IdentityMode.COMET -> {
                        Text(
                            text = "comet · temporary",
                            style = NPType.bodySm,
                            color = colors.foregroundDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    displayParent != null -> {
                        Text(
                            text = "under $displayParent",
                            style = NPType.bodySm,
                            color = colors.foregroundDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            StatusChip(state = if (isConfigured) uiState.runtimeStatus.state else RuntimeState.UNINITIALIZED)
        }

        Spacer(modifier = Modifier.height(NPSpacing.md))

        NPButton(
            text = if (isConfigured) "Add identity" else "Set up moon",
            onClick = onNavigateToOnboarding,
            style = if (isConfigured) NPButtonStyle.SECONDARY else NPButtonStyle.FILLED
        )

        Spacer(modifier = Modifier.height(NPSpacing.xs))

        Text(
            text = if (isConfigured) {
                "Pair another moon or use a moon key."
            } else {
                "Pair with your parent ship or use a moon key."
            },
            style = NPType.caption,
            color = colors.foregroundDim
        )

        if (uiState.demoIdentity.isConfigured) {
            Spacer(modifier = Modifier.height(NPSpacing.sm))
            Text(
                text = "reset demo identity",
                style = NPType.bodySm,
                color = colors.foregroundFaint,
                modifier = Modifier
                    .clickable { viewModel.resetDemo() }
                    .padding(vertical = NPSpacing.sm)
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        // Runtime details
        RuntimeDetailsPanel(runtimeStatus = uiState.runtimeStatus)

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        // Boot package status
        BootPackagePanel(bootPackageStatus = uiState.bootPackageStatus)

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        // Network status
        NetworkStatusPanel(networkStatus = uiState.networkStatus)

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        // Diagnostics
        DiagnosticsPanel(diagnostics = uiState.diagnostics)

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        // Debug status line
        val runtimeLabel = uiState.runtimeStatus.state.name.lowercase()
        val identityLabel = uiState.demoIdentity.modeLabel
        val backendLabel = if (uiState.controllerAvailable) "provider" else "demo"

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.backgroundSecondary)
                .padding(NPSpacing.sm)
        ) {
            Text(
                text = "debug: $backendLabel · $identityLabel · $runtimeLabel",
                style = NPType.nano,
                color = colors.foregroundFaint
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.lg))
    }
}
