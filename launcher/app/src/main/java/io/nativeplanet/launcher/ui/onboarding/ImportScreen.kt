package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.*
import kotlinx.coroutines.delay

@Composable
fun ImportScreen(
    onImportComplete: (shipName: String, parentName: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    var importState by remember { mutableStateOf(ImportState.CHOOSE_SOURCE) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(importState) {
        if (importState == ImportState.IMPORTING) {
            statusMessage = "reading key file…"
            delay(1000)
            statusMessage = "validating identity…"
            delay(1000)
            statusMessage = "importing ship…"
            delay(1500)
            importState = ImportState.COMPLETE
            onImportComplete("~ridlur-figbud", "~marzod")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(NPSpacing.screenGutter)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        Text(
            text = "IMPORT SHIP",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Bring your identity.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        Text(
            text = "Import an existing Urbit ship from a backup or key file. Your ship's data and identity will be restored.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxl))

        when (importState) {
            ImportState.CHOOSE_SOURCE -> {
                ImportOption(
                    title = "Scan QR code",
                    description = "Scan a backup QR from another device",
                    onClick = { importState = ImportState.IMPORTING }
                )

                Spacer(modifier = Modifier.height(NPSpacing.md))

                ImportOption(
                    title = "Select key file",
                    description = "Import from .key or pier backup",
                    onClick = { importState = ImportState.IMPORTING }
                )

                Spacer(modifier = Modifier.height(NPSpacing.md))

                ImportOption(
                    title = "Enter manually",
                    description = "Type your master ticket or seed phrase",
                    onClick = { importState = ImportState.IMPORTING }
                )
            }

            ImportState.IMPORTING -> {
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = colors.accent,
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.height(NPSpacing.md))

                    Text(
                        text = statusMessage,
                        style = NPType.caption,
                        color = colors.foregroundDim
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            ImportState.COMPLETE -> {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "imported!",
                    style = NPType.caption,
                    color = colors.accent,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (importState == ImportState.CHOOSE_SOURCE) {
            Text(
                text = "← back",
                style = NPType.bodySm,
                color = colors.foregroundFaint,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(NPSpacing.sm)
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.lg))
    }
}

@Composable
private fun ImportOption(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(colors.backgroundSecondary)
            .clickable(onClick = onClick)
            .padding(NPSpacing.cardPadding)
    ) {
        Text(
            text = title,
            style = NPType.bodyLg,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.xs))

        Text(
            text = description,
            style = NPType.bodySm,
            color = colors.foregroundDim
        )
    }
}

private enum class ImportState {
    CHOOSE_SOURCE,
    IMPORTING,
    COMPLETE
}
