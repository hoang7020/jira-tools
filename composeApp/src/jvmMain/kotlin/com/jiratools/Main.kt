package com.jiratools

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jiratools.app.App
import com.jiratools.app.AppContainer
import com.jiratools.platform.JvmAppClock
import com.jiratools.platform.JvmAppPaths
import com.jiratools.platform.JvmSecureStorage
import com.jiratools.platform.JvmUrlOpener
import com.jiratools.ui.layout.AppWindowDefaults
import java.awt.Dimension
import kotlin.math.roundToInt

fun main() =
    application {
        val container =
            remember {
                AppContainer(
                    appPaths = JvmAppPaths(),
                    secureStorage = JvmSecureStorage(),
                    urlOpener = JvmUrlOpener(),
                    appClock = JvmAppClock(),
                )
            }
        val windowState =
            rememberWindowState(
                placement = WindowPlacement.Maximized,
                size = AppWindowDefaults.RESTORE_SIZE,
            )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Jira Tools",
        ) {
            LaunchedEffect(window) {
                window.minimumSize = AppWindowDefaults.MIN_SIZE.toAwtDimension()
            }
            App(container)
        }
    }

/**
 * Đổi kích thước thiết kế sang đơn vị hình học cửa sổ AWT.
 *
 * Trong Compose Desktop 1 dp trùng 1 đơn vị AWT (điểm ảnh logic), nên chỉ cần làm tròn — xem
 * `research.md` mục 1.
 */
private fun DpSize.toAwtDimension() = Dimension(width.value.roundToInt(), height.value.roundToInt())
