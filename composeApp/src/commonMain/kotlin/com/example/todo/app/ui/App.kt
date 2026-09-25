package com.example.todo.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

sealed interface Screen {
    data object TaskList : Screen
    data class TaskEditor(val taskId: Long?) : Screen
    data object Categories : Screen
}

@Composable
fun TodoApp(onExitApp: () -> Unit = {}) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TaskList) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = currentScreen) {
            is Screen.TaskList -> TaskListScreen(
                onEditTask = { currentScreen = Screen.TaskEditor(it) },
                onManageCategories = { currentScreen = Screen.Categories },
                onExitApp = onExitApp,
            )
            is Screen.TaskEditor -> TaskEditorScreen(
                taskId = screen.taskId,
                onBack = { currentScreen = Screen.TaskList },
            )
            is Screen.Categories -> CategoriesScreen(
                onBack = { currentScreen = Screen.TaskList },
            )
        }
    }
}
