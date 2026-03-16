package com.shotmetrics.app.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shotmetrics.app.ui.editor.EditorAction
import com.shotmetrics.app.ui.editor.EditorMode
import com.shotmetrics.app.ui.editor.EditorState

@Composable
fun EditorToolbar(
    state: EditorState,
    onAction: (EditorAction) -> Unit,
    onSave: () -> Unit,
    onExportImage: () -> Unit = {},
    onExportCSV: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp).navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeButton(
                    icon = Icons.Default.Straighten,
                    label = "Reference",
                    isSelected = state.mode == EditorMode.REFERENCE,
                    onClick = { onAction(EditorAction.SetMode(EditorMode.REFERENCE)) }
                )
                ModeButton(
                    icon = Icons.Default.CenterFocusStrong,
                    label = "POA",
                    isSelected = state.mode == EditorMode.POA,
                    onClick = { onAction(EditorAction.SetMode(EditorMode.POA)) }
                )
                ModeButton(
                    icon = Icons.Default.AdsClick,
                    label = "Impacts",
                    isSelected = state.mode == EditorMode.IMPACT,
                    onClick = { onAction(EditorAction.SetMode(EditorMode.IMPACT)) }
                )

                // Guide line toggle
                ModeButton(
                    icon = Icons.Default.Edit,
                    label = "Guide",
                    isSelected = state.isDrawingGuideLines,
                    onClick = { onAction(EditorAction.ToggleGuideLineMode) }
                )

                if (state.guideLines.isNotEmpty()) {
                    IconButton(onClick = { onAction(EditorAction.ClearGuideLines) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Lines", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = { onAction(EditorAction.Undo) }) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { onAction(EditorAction.Redo) }) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onAction(EditorAction.ToggleResultsPanel) },
                    enabled = state.canCalculate
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = "Results")
                }
                FilledTonalIconButton(
                    onClick = { onAction(EditorAction.ToggleATZPanel) },
                    enabled = state.canCalculateATZ
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "ATZ")
                }
                FilledTonalIconButton(onClick = onSave) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
                FilledTonalIconButton(
                    onClick = onExportImage,
                    enabled = state.canCalculate
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Export Image")
                }
                FilledTonalIconButton(
                    onClick = onExportCSV,
                    enabled = state.canCalculate
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export CSV")
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "modeButtonColor_$label"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        label = "modeButtonContentColor_$label"
    )

    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}
