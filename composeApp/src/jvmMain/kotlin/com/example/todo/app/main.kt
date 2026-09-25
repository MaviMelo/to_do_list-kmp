package com.example.todo.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material3.MaterialTheme
import com.example.todo.app.ui.AppGraph
import com.example.todo.app.ui.TodoApp
import com.example.todo.shared.createDependencies

fun main() {
    AppGraph.init(createDependencies())
    application {
        Window(onCloseRequest = ::exitApplication, title = "To-Do (KMP)") {
            MaterialTheme {
                TodoApp()
            }
        }
    }
}
