package re.alwyn974.inventory

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Inventory",
    ) {
        App()
    }
}