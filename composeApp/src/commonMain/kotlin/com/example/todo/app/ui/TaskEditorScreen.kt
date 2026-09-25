package com.example.todo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.todo.shared.TaskEditorViewModel
import com.example.todo.shared.currentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    taskId: Long?,
    onBack: () -> Unit,
) {
    val viewModel = remember { TaskEditorViewModel(AppGraph.repository) }
    val state by viewModel.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) { viewModel.load(taskId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Nova Tarefa" else "Editar Tarefa") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←") }
                },
                actions = {
                    if (!state.isNew) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("Excluir", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Título *") },
                isError = state.titleError,
                supportingText = if (state.titleError) {
                    { Text("O título é obrigatório") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            // Vencimento
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.hasDue, onCheckedChange = viewModel::onHasDueChange)
                Text("Com vencimento (data e hora)")
            }

            if (state.hasDue) {
                DuePicker(
                    epochMillis = state.dueDateTime ?: currentTimeMillis() + 60_000L,
                    onChange = viewModel::onDueChange,
                )
            }

            // Categoria
            Text("Categoria", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.categoryId == null,
                    onClick = { viewModel.onCategoryChange(null) },
                    label = { Text("Nenhuma") },
                )
                state.categories.forEach { cat ->
                    FilterChip(
                        selected = state.categoryId == cat.id,
                        onClick = { viewModel.onCategoryChange(cat.id) },
                        label = { Text(cat.name) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (viewModel.save()) onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isNew) "Salvar" else "Salvar alterações")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir tarefa") },
            text = { Text("Excluir \"${state.title}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    AppGraph.taskDeleter(state.taskId)
                    confirmDelete = false
                    onBack()
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DuePicker(epochMillis: Long, onChange: (Long) -> Unit) {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val key = epochMillis / (24 * 3600 * 1000L) // re-sincroniza só quando o dia muda
    var year by remember(key) { mutableStateOf(local.year) }
    var month by remember(key) { mutableStateOf(local.monthNumber) }
    var day by remember(key) { mutableStateOf(local.dayOfMonth) }
    var hour by remember(key) { mutableStateOf(local.hour) }
    var minute by remember(key) { mutableStateOf(local.minute) }

    val monthNames = listOf(
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez",
    )

    // Compose Multiplatform 1.6 não tem DatePicker/TimePicker multiplataforma nativos.
    Column {
        Text(
            "Vence em: %02d/%02d/%04d às %02d:%02d".format(day, month, year, hour, minute),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper(label = "Dia", value = day, range = 1..daysInMonth(year, month), onChange = { day = it })
            MonthDropdown(
                monthNames = monthNames,
                selected = month,
                onSelect = { m ->
                    month = m
                    day = day.coerceIn(1, daysInMonth(year, m))
                },
            )
            Stepper(
                label = "Ano",
                value = year,
                range = local.year - 1..local.year + 5,
                onChange = { year = it },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper(label = "Hora", value = hour, range = 0..23, onChange = { hour = it })
            Stepper(label = "Min", value = minute, range = 0..59, onChange = { minute = it })
        }
    }

    LaunchedEffect(year, month, day, hour, minute) {
        onChange(toEpochMillis(year, month, day, hour, minute))
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    display: ((Int) -> String)? = null,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("-") }
            Text(display?.invoke(value) ?: "$value", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+") }
        }
    }
}

@Composable
private fun MonthDropdown(
    monthNames: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mês", style = MaterialTheme.typography.labelSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(monthNames[selected - 1])
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                monthNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(index + 1)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
}

private fun toEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    val clampedDay = day.coerceIn(1, daysInMonth(year, month))
    return LocalDateTime(year, month, clampedDay, hour, minute)
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}
