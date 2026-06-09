package io.nativeplanet.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.theme.NativePlanetTheme

@Composable
fun NPTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isSecret: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true
) {
    val colors = NativePlanetTheme.colors
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        !enabled -> colors.hairline
        isFocused -> colors.foregroundDim
        else -> colors.hairline
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = NPType.caption,
            color = if (isFocused) colors.foreground else colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xs))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            enabled = enabled,
            textStyle = NPType.bodySm.copy(color = colors.foreground),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isSecret) KeyboardType.Password else KeyboardType.Text
            ),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(
                            color = colors.backgroundSecondary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(NPSpacing.md)
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = NPType.bodySm,
                            color = colors.foregroundFaint
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun NPTextFieldMultiline(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isSecret: Boolean = false,
    minLines: Int = 3,
    maxLines: Int = 5,
    enabled: Boolean = true
) {
    NPTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        isSecret = isSecret,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled
    )
}
