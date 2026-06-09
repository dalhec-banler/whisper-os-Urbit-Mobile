package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.ControlResult
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPTextField
import io.nativeplanet.launcher.ui.components.NPTextFieldMultiline
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(
    onImportComplete: (shipName: String, parentName: String?) -> Unit,
    onBack: () -> Unit,
    onProvisionMoon: suspend (shipName: String, parentName: String, keyMaterial: String) -> ControlResult,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors
    val scope = rememberCoroutineScope()

    var importState by remember { mutableStateOf(ImportState.ENTER_MANUAL) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shipName by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var keyMaterial by remember { mutableStateOf("") }

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
            text = "MOON KEY",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Manual import.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        Text(
            text = "Enter a throwaway moon key from your parent planet. Generate one with |moon in your planet's dojo.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxl))

        when (importState) {
            ImportState.ENTER_MANUAL -> {
                NPTextField(
                    value = shipName,
                    onValueChange = { shipName = it },
                    label = "Moon",
                    placeholder = "~sample-moon-parent"
                )

                Spacer(modifier = Modifier.height(NPSpacing.md))

                NPTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    label = "Parent",
                    placeholder = "~parent-planet"
                )

                Spacer(modifier = Modifier.height(NPSpacing.md))

                NPTextFieldMultiline(
                    value = keyMaterial,
                    onValueChange = { keyMaterial = it },
                    label = "Moon key",
                    placeholder = "0w...",
                    isSecret = true
                )

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(NPSpacing.md))
                    Text(
                        text = message,
                        style = NPType.caption,
                        color = colors.error
                    )
                }

                Spacer(modifier = Modifier.height(NPSpacing.xl))

                val canImport = shipName.trim().isNotEmpty() &&
                    parentName.trim().isNotEmpty() &&
                    keyMaterial.trim().isNotEmpty()

                NPButton(
                    text = "Import moon",
                    enabled = canImport,
                    onClick = {
                        val submittedShip = shipName.trim()
                        val submittedParent = parentName.trim()
                        val submittedKey = keyMaterial.trim()
                        keyMaterial = ""
                        errorMessage = null
                        statusMessage = "validating identity…"
                        importState = ImportState.IMPORTING

                        scope.launch {
                            val result = onProvisionMoon(submittedShip, submittedParent, submittedKey)
                            when (result) {
                                is ControlResult.Success -> {
                                    statusMessage = "starting moon…"
                                    importState = ImportState.COMPLETE
                                    onImportComplete(
                                        normalizeShip(submittedShip),
                                        normalizeShip(submittedParent)
                                    )
                                }
                                is ControlResult.AlreadyInState -> {
                                    statusMessage = "already ${result.state.name.lowercase()}"
                                    importState = ImportState.COMPLETE
                                    onImportComplete(
                                        normalizeShip(submittedShip),
                                        normalizeShip(submittedParent)
                                    )
                                }
                                is ControlResult.Failed -> {
                                    errorMessage = "${result.code}: ${result.message}"
                                    importState = ImportState.ENTER_MANUAL
                                }
                            }
                        }
                    }
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

        if (importState == ImportState.ENTER_MANUAL) {
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

private fun normalizeShip(ship: String): String {
    val trimmed = ship.trim()
    return if (trimmed.startsWith("~")) trimmed else "~$trimmed"
}

private enum class ImportState {
    ENTER_MANUAL,
    IMPORTING,
    COMPLETE
}
