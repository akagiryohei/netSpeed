package dev.akagiryohei.netspeed

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NetSpeed - ネットワーク速度モニター",
        state = rememberWindowState(width = 420.dp, height = 600.dp),
    ) {
        App()
    }
}
