package com.shotmetrics.app.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shotmetrics.app.domain.model.Calibers
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.ui.editor.components.EditorToolbar
import com.shotmetrics.app.ui.editor.components.TargetCanvas
import com.shotmetrics.app.ui.export.ExportUtils
import com.shotmetrics.app.ui.results.ATZPanel
import com.shotmetrics.app.ui.results.ResultsPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: String?,
    sessionId: Long?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onExportPreview: ((EditorState) -> Unit)? = null,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.initialize(imageUri, sessionId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(EditorAction.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.caliber.isNotBlank()) state.caliber else "New Session",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SessionInfoBar(
                state = state,
                onDistanceChange = { viewModel.onAction(EditorAction.SetDistance(it)) },
                onCaliberChange = { viewModel.onAction(EditorAction.SetCaliber(it)) },
                onDistanceUnitChange = { viewModel.onAction(EditorAction.SetDistanceUnit(it)) }
            )

            Box(modifier = Modifier.weight(1f).clipToBounds()) {
                TargetCanvas(
                    state = state,
                    onCenterPointChanged = { viewModel.onAction(EditorAction.UpdatePendingPoint(it)) }
                )

                // Bottom action buttons row
                val showConfirm = state.needsPlacement
                val showFinish = state.canCalculate && !state.showOverlays && !state.isDrawingGuideLines
                if (showConfirm || showFinish) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        if (showConfirm) {
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.onAction(EditorAction.ConfirmPlacement) },
                                icon = { Icon(Icons.Default.Check, "Confirm") },
                                text = { Text(state.confirmButtonText) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        if (showFinish) {
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.onAction(EditorAction.FinishPlacement) },
                                icon = { Icon(Icons.Default.Done, "Finish") },
                                text = { Text("Finish") },
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }

                // Placement hint
                if (state.placementHint.isNotBlank()) {
                    Text(
                        text = state.placementHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.55f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                if (state.showResultsPanel && state.ballisticsResult != null) {
                    ResultsPanel(
                        result = state.ballisticsResult!!,
                        angleUnit = state.angleUnit,
                        lengthUnit = state.lengthUnit,
                        overlayVisibility = state.overlayVisibility,
                        impacts = state.impacts,
                        onToggleOverlay = { viewModel.onAction(EditorAction.ToggleOverlay(it)) },
                        onToggleImpact = { viewModel.onAction(EditorAction.ToggleImpact(it)) },
                        onRemoveImpact = { viewModel.onAction(EditorAction.RemoveImpact(it)) },
                        onDismiss = { viewModel.onAction(EditorAction.ToggleResultsPanel) }
                    )
                }

                if (state.showATZPanel && state.atzResult != null) {
                    ATZPanel(
                        result = state.atzResult!!,
                        onDismiss = { viewModel.onAction(EditorAction.ToggleATZPanel) }
                    )
                }
            }

            EditorToolbar(
                state = state,
                onAction = viewModel::onAction,
                onSave = {
                    viewModel.saveSession()
                    Toast.makeText(context, "Session saved", Toast.LENGTH_SHORT).show()
                },
                onExportImage = {
                    if (onExportPreview != null) {
                        onExportPreview(state)
                    } else {
                        ExportUtils.exportImage(context, state) { uri ->
                            if (uri != null) {
                                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                                ExportUtils.shareFile(context, uri, "image/png")
                            } else {
                                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onExportCSV = {
                    val result = state.ballisticsResult
                    if (result != null) {
                        val uri = ExportUtils.exportCSV(context, state, result)
                        if (uri != null) {
                            ExportUtils.shareFile(context, uri, "text/csv")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionInfoBar(
    state: EditorState,
    onDistanceChange: (Double) -> Unit,
    onCaliberChange: (String) -> Unit,
    onDistanceUnitChange: (DistanceUnit) -> Unit
) {
    var distText by remember(state.distanceToTarget) {
        mutableStateOf(
            if (state.distanceToTarget == state.distanceToTarget.toLong().toDouble())
                state.distanceToTarget.toLong().toString()
            else state.distanceToTarget.toString()
        )
    }
    var showCaliberPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = distText,
            onValueChange = { input ->
                distText = input
                input.toDoubleOrNull()?.let { if (it > 0) onDistanceChange(it) }
            },
            label = { Text("Distance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = {
                Row {
                    DistanceUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = state.distanceUnit == unit,
                            onClick = { onDistanceUnitChange(unit) },
                            label = { Text(unit.abbreviation) }
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                }
            }
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = state.caliber.ifBlank { "Tap to select" },
            onValueChange = {},
            label = { Text("Caliber") },
            modifier = Modifier.weight(1f).clickable { showCaliberPicker = true },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                Text(
                    "\u25BC",
                    modifier = Modifier.clickable { showCaliberPicker = true },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )
    }

    if (showCaliberPicker) {
        CaliberPickerSheet(
            onSelect = { name ->
                onCaliberChange(name)
                showCaliberPicker = false
            },
            onDismiss = { showCaliberPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaliberPickerSheet(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var caliberSearch by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Caliber", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = caliberSearch,
                onValueChange = { caliberSearch = it },
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            val grouped = Calibers.search(caliberSearch).groupBy { it.category }
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                grouped.forEach { (category, calibers) ->
                    item(key = "header_$category") {
                        Text(
                            category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(calibers, key = { it.name }) { cal ->
                        Text(
                            cal.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cal.name) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
