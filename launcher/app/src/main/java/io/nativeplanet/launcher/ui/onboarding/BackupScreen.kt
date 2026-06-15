package io.nativeplanet.launcher.ui.onboarding

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.IdentityMode
import io.nativeplanet.launcher.theme.NPColors
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.theme.NativePlanetTheme
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPButtonStyle

@Composable
fun BackupScreen(
    shipName: String,
    parentName: String?,
    identityMode: IdentityMode,
    keyMaterial: String?,
    onEnterWhisperOs: () -> Unit,
    onBackToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors
    val context = LocalContext.current
    var acknowledged by remember { mutableStateOf(false) }

    val hasKeyMaterial = !keyMaterial.isNullOrBlank()
    val isImport = identityMode == IdentityMode.IMPORTED

    // Apply FLAG_SECURE when key material is shown to prevent screenshots
    if (hasKeyMaterial) {
        DisposableEffect(Unit) {
            val activity = context as? ComponentActivity
            activity?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(NPSpacing.screenPadXWide)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        Text(
            text = "BACKUP · 4 OF 4 · KEEP THIS SAFE",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Your satellite key.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Text(
            text = "This is the only way to restore your satellite to a new device.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxl))

        when {
            hasKeyMaterial -> {
                // Show actual key material in secure container
                KeyMaterialDisplay(keyMaterial = keyMaterial!!)

                Spacer(modifier = Modifier.height(NPSpacing.xl))

                Text(
                    text = "Write this down or save it somewhere safe. Do not share it with anyone.",
                    style = NPType.bodySm,
                    color = colors.foregroundDim
                )
            }

            isImport -> {
                // User imported with their own key — they already have it
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 1.dp,
                            color = NPColors.accentAmberSoft.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(NPColors.accentAmberSoft.copy(alpha = 0.08f))
                        .padding(NPSpacing.lg)
                ) {
                    Text(
                        text = "You imported this satellite with your existing key. Make sure you still have it stored safely.",
                        style = NPType.bodySm,
                        color = colors.foreground,
                        textAlign = TextAlign.Start
                    )
                }
            }

            else -> {
                // Key material not available from backend yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 1.dp,
                            color = NPColors.inkFaint,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(NPColors.inkHair)
                        .padding(NPSpacing.lg)
                ) {
                    Column {
                        Text(
                            text = "Key backup not yet available",
                            style = NPType.caption,
                            color = colors.foreground
                        )
                        Spacer(modifier = Modifier.height(NPSpacing.sm))
                        Text(
                            text = "Your satellite key will be available for backup in a future update. For now, you can continue to Whisper OS.",
                            style = NPType.bodySm,
                            color = colors.foregroundDim
                        )
                    }
                }

                Spacer(modifier = Modifier.height(NPSpacing.lg))

                Text(
                    text = "If you need to back up your key now, import your satellite manually instead.",
                    style = NPType.bodySm,
                    color = colors.foregroundFaint
                )

                Spacer(modifier = Modifier.height(NPSpacing.md))

                Text(
                    text = "← import manually",
                    style = NPType.bodySm,
                    color = NPColors.accentAmberSoft,
                    modifier = Modifier
                        .clickable(onClick = onBackToImport)
                        .padding(NPSpacing.sm)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Acknowledgment checkbox for key material or import
        if (hasKeyMaterial || isImport) {
            AcknowledgmentRow(
                checked = acknowledged,
                onCheckedChange = { acknowledged = it },
                text = if (hasKeyMaterial) {
                    "I have saved my satellite key"
                } else {
                    "I have my satellite key stored safely"
                }
            )

            Spacer(modifier = Modifier.height(NPSpacing.xl))

            NPButton(
                text = "enter whisper os",
                enabled = acknowledged,
                onClick = onEnterWhisperOs
            )
        } else {
            // No key material and not import — allow continuing with honest UI
            NPButton(
                text = "continue to whisper os",
                onClick = onEnterWhisperOs,
                style = NPButtonStyle.SECONDARY
            )

            Spacer(modifier = Modifier.height(NPSpacing.sm))

            Text(
                text = "You can back up your key later from Settings → Identity.",
                style = NPType.nano,
                color = colors.foregroundFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xxxl))
    }
}

@Composable
private fun KeyMaterialDisplay(keyMaterial: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = NPColors.accentAmberSoft,
                shape = RoundedCornerShape(4.dp)
            )
            .background(NPColors.deep)
            .padding(NPSpacing.lg)
    ) {
        Text(
            text = keyMaterial,
            style = NPType.caption,
            color = NPColors.paper,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AcknowledgmentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String
) {
    val colors = NativePlanetTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .border(
                width = 1.dp,
                color = if (checked) NPColors.accentAmberSoft else NPColors.inkFaint,
                shape = RoundedCornerShape(4.dp)
            )
            .background(if (checked) NPColors.accentAmberSoft.copy(alpha = 0.1f) else NPColors.inkHair)
            .padding(NPSpacing.md)
    ) {
        Text(
            text = if (checked) "✓ $text" else "○ $text",
            style = NPType.bodySm,
            color = if (checked) colors.foreground else colors.foregroundDim
        )
    }
}
