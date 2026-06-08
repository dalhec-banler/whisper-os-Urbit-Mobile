package io.nativeplanet.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.NetworkStatus
import io.nativeplanet.launcher.domain.model.NetworkType
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NetworkStatusPanel(
    networkStatus: NetworkStatus,
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
        SectionHeader(text = "Network")

        Spacer(modifier = Modifier.height(NPSpacing.md))

        InfoRow(
            label = "Type",
            value = networkStatus.type.name,
            valueColor = if (networkStatus.type == NetworkType.NONE) colors.error else colors.foreground
        )

        networkStatus.interfaceName?.let {
            InfoRow(label = "Interface", value = it)
        }

        InfoRow(
            label = "Validated",
            value = if (networkStatus.validated) "yes" else "no",
            valueColor = if (networkStatus.validated) NPColors.accentSage else colors.foregroundDim
        )

        if (networkStatus.dnsServers.isNotEmpty()) {
            InfoRow(label = "DNS", value = networkStatus.dnsServers.joinToString(", "))
        }

        networkStatus.nat64Prefix?.let {
            InfoRow(label = "NAT64", value = it)
        }

        InfoRow(
            label = "Resolver",
            value = if (networkStatus.resolverAvailable) "available" else "unavailable",
            valueColor = if (networkStatus.resolverAvailable) NPColors.accentSage else colors.error
        )

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date(networkStatus.timestampMs))
        Text(
            text = "updated $timestamp",
            style = NPType.nano,
            color = colors.foregroundFaint
        )
    }
}

@Composable
private fun InfoRow(
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
