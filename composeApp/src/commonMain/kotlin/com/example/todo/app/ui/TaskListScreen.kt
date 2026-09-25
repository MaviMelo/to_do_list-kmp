package com.example.todo.app.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todo.shared.Category
import com.example.todo.shared.StatusFilter
import com.example.todo.shared.TaskFilter
import com.example.todo.shared.TaskWithCategory
import com.example.todo.shared.TodoViewModel
import com.example.todo.shared.db.TodoTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onEditTask: (Long?) -> Unit,
    onManageCategories: () -> Unit,
) {
    val viewModel = remember { TodoViewModel(AppGraph.repository) }
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var taskToDelete by remember { mutableStateOf<TodoTask?>(null) }

    LaunchedEffect(Unit) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Tarefas") },
                actions = {
                    TextButton(onClick = onManageCategories) {
                        Text("Categorias")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditTask(null) },
                modifier = Modifier.semantics { contentDescription = "Nova tarefa" },
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Filtros: status
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusFilter.entries.forEach { status ->
                    FilterChip(
                        selected = filter.status == status,
                        onClick = { viewModel.setFilter(filter.copy(status = status)) },
                        label = {
                            Text(
                                when (status) {
                                    StatusFilter.ALL -> "Todas"
                                    StatusFilter.PENDING -> "Pendentes"
                                    StatusFilter.COMPLETED -> "Concluídas"
                                },
                            )
                        },
                    )
                }
            }

            // Filtros: categorias
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = filter.categoryId == null,
                        onClick = { viewModel.setFilter(filter.copy(categoryId = null)) },
                        label = { Text("Todas") },
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = filter.categoryId == cat.id,
                            onClick = { viewModel.setFilter(filter.copy(categoryId = cat.id)) },
                            label = { Text(cat.name) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nenhuma tarefa encontrada.\nUse o botão + para adicionar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tasks, key = { it.task.id }) { item ->
                        TaskRow(
                            item = item,
                            onToggle = { viewModel.toggleCompleted(item.task) },
                            onEdit = { onEditTask(item.task.id) },
                            onDelete = { taskToDelete = item.task },
                        )
                    }
                }
            }
        }
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Excluir tarefa") },
            text = { Text("Excluir \"${task.title}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    taskToDelete = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TaskRow(
    item: TaskWithCategory,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val task = item.task
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.category?.let { cat ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(parseColor(cat.color), CircleShape),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                task.dueDateTime?.let { due ->
                    val label = formatDueLabel(due, task.completed)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            task.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> Color(0xFFD32F2F)
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("✕")
        }
    }
}

private fun formatDueLabel(epochMillis: Long, completed: Boolean): String =
    formatDue(epochMillis, completed)
