package com.shotmetrics.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shotmetrics.app.ui.camera.CameraScreen
import com.shotmetrics.app.ui.editor.EditorScreen
import com.shotmetrics.app.ui.editor.EditorState
import com.shotmetrics.app.ui.export.ExportPreviewScreen
import com.shotmetrics.app.ui.home.HomeScreen
import com.shotmetrics.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val EDITOR = "editor/{imageUri}"
    const val EDITOR_SESSION = "editor_session/{sessionId}"
    const val SETTINGS = "settings"
    const val EXPORT_PREVIEW = "export_preview"

    fun editor(imageUri: String): String =
        "editor/${Uri.encode(imageUri)}"

    fun editorSession(sessionId: Long): String =
        "editor_session/$sessionId"
}

/**
 * Holds EditorState temporarily when navigating from Editor to ExportPreview.
 * Cleared after the ExportPreviewScreen reads it.
 */
object ExportStateHolder {
    var editorState: EditorState? = null
}

@Composable
fun ShotMetricsNavGraph() {
    val navController = rememberNavController()

    fun safeBack() {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewSession = { navController.navigate(Routes.CAMERA) },
                onOpenSession = { id -> navController.navigate(Routes.editorSession(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onImageCaptured = { uri ->
                    navController.navigate(Routes.editor(uri.toString())) {
                        popUpTo(Routes.CAMERA) { inclusive = true }
                    }
                },
                onImageSelected = { uri ->
                    navController.navigate(Routes.editor(uri.toString())) {
                        popUpTo(Routes.CAMERA) { inclusive = true }
                    }
                },
                onBack = { safeBack() }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val imageUri = Uri.decode(encodedUri)
            EditorScreen(
                imageUri = imageUri,
                sessionId = null,
                onBack = { safeBack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onExportPreview = { state ->
                    ExportStateHolder.editorState = state
                    navController.navigate(Routes.EXPORT_PREVIEW)
                }
            )
        }

        composable(
            route = Routes.EDITOR_SESSION,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            EditorScreen(
                imageUri = null,
                sessionId = sessionId,
                onBack = { safeBack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onExportPreview = { state ->
                    ExportStateHolder.editorState = state
                    navController.navigate(Routes.EXPORT_PREVIEW)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { safeBack() })
        }

        composable(Routes.EXPORT_PREVIEW) {
            val editorState = ExportStateHolder.editorState
            if (editorState != null) {
                ExportPreviewScreen(
                    editorState = editorState,
                    onBack = { safeBack() }
                )
            } else {
                safeBack()
            }
        }
    }
}
