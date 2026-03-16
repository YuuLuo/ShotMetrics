package com.shotmetrics.app.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.BallisticsResult
import com.shotmetrics.app.domain.model.ImpactPoint
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.ui.editor.OverlayType
import com.shotmetrics.app.ui.editor.OverlayVisibility
import java.util.Locale

@Composable
fun ResultsPanel(
    result: BallisticsResult,
    angleUnit: AngleUnit,
    lengthUnit: LengthUnit,
    overlayVisibility: OverlayVisibility,
    impacts: List<ImpactPoint>,
    onToggleOverlay: (OverlayType) -> Unit,
    onToggleImpact: (Int) -> Unit,
    onRemoveImpact: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOverlayControls by remember { mutableStateOf(false) }
    var showImpactSelector by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Statistics", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            Spacer(Modifier.height(8.dp))

            StatRow("Impacts", "${result.impactCount}")
            StatRow("Group Size", formatAngle(result.groupSizeMoa, result.groupSizeMil, angleUnit))
            StatRow("Extreme Spread", formatLength(result.extremeSpread, lengthUnit))
            StatRow("Mean Radius", formatLength(result.meanRadius, lengthUnit))
            StatRow("CEP", formatLength(result.cep, lengthUnit))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            StatRow("Radial SD", formatLength(result.radialSD, lengthUnit))
            StatRow("Vertical SD", formatLength(result.verticalSD, lengthUnit))
            StatRow("Horizontal SD", formatLength(result.horizontalSD, lengthUnit))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            StatRow("Mean Windage", formatAngleValue(result.meanWindage, angleUnit))
            StatRow("Mean Elevation", formatAngleValue(result.meanElevation, angleUnit))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ExpandableSection(
                title = "Overlay Controls",
                expanded = showOverlayControls,
                onToggle = { showOverlayControls = !showOverlayControls }
            ) {
                OverlayToggle("Extreme Spread", overlayVisibility.extremeSpread, Color(0xFFFF5722)) {
                    onToggleOverlay(OverlayType.EXTREME_SPREAD)
                }
                OverlayToggle("CEP", overlayVisibility.cep, Color(0xFF9C27B0)) {
                    onToggleOverlay(OverlayType.CEP)
                }
                OverlayToggle("Mean Radius", overlayVisibility.meanRadius, Color(0xFF00BCD4)) {
                    onToggleOverlay(OverlayType.MEAN_RADIUS)
                }
                OverlayToggle("MPI", overlayVisibility.mpi, Color(0xFFFF9800)) {
                    onToggleOverlay(OverlayType.MPI)
                }
                OverlayToggle("Offset Line", overlayVisibility.offsetLine, Color(0xFFFFEB3B)) {
                    onToggleOverlay(OverlayType.OFFSET_LINE)
                }
            }

            ExpandableSection(
                title = "Impact Selector (${impacts.count { it.enabled }}/${impacts.size})",
                expanded = showImpactSelector,
                onToggle = { showImpactSelector = !showImpactSelector }
            ) {
                impacts.forEach { impact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleImpact(impact.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = impact.enabled,
                            onCheckedChange = { onToggleImpact(impact.id) }
                        )
                        Text(
                            "Impact #${impact.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveImpact(impact.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand"
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(start = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun OverlayToggle(
    label: String,
    isVisible: Boolean,
    indicatorColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = indicatorColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = isVisible, onCheckedChange = { onToggle() })
    }
}

private fun formatLength(value: Double, unit: LengthUnit): String {
    return String.format(Locale.US, "%.3f %s", value, unit.abbreviation)
}

private fun formatAngle(moa: Double, mil: Double, displayUnit: AngleUnit): String {
    return when (displayUnit) {
        AngleUnit.MOA -> String.format(Locale.US, "%.2f MOA", moa)
        AngleUnit.MIL -> String.format(Locale.US, "%.2f MIL", mil)
    }
}

private fun formatAngleValue(value: Double, unit: AngleUnit): String {
    val sign = if (value >= 0) "+" else ""
    return String.format(Locale.US, "%s%.2f %s", sign, value, unit.abbreviation)
}
