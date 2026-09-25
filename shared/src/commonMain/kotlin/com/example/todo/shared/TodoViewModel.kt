package com.example.todo.shared

import app.cash.sqldelight.coroutines.asFlow
import com.example.todo.shared.db.TodoTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskFilter(
    val status: StatusFilter = StatusFilter.ALL,
    val categoryId: Long? = null, // null = todas
)

enum class StatusFilter { ALL, PENDING, COMPLETED }

data class TaskWithCategory(
    val task: TodoTask,
    val category: Category?,
)

data class TaskListState(
    val tasks: List<TaskWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TaskFilter = TaskFilter(),
)

class TodoViewModel(private val repository: TodoRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _filter = MutableStateFlow(TaskFilter())
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.categories()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gatilho de refresh: incrementado após cada escrita para re-emitir a lista
    // mesmo que a invalidação automática de queries do driver não dispare.
    private val _refresh = MutableStateFlow(0)

    // Sincroniza com escritas feitas fora desta instância (editor de tarefas,
    // tela de categorias), que usam TodoViewModel.refreshNow().
    private val _external: MutableStateFlow<Int> = TodoViewModel.Companion._globalRefresh

    val tasks: StateFlow<List<TaskWithCategory>> = combine(
        _refresh,
        _external,
        repository.tasks(),
        repository.categories(),
        _filter,
    ) { _, _, tasks, cats, filter ->
        val byId = cats.associateBy { it.id }
        tasks
            .map { TaskWithCategory(it, it.categoryId?.let { c -> byId[c] }) }
            .filter { twc ->
                val statusOk = when (filter.status) {
                    StatusFilter.ALL -> true
                    StatusFilter.PENDING -> !twc.task.completed
                    StatusFilter.COMPLETED -> twc.task.completed
                }
                val categoryOk = filter.categoryId == null || twc.task.categoryId == filter.categoryId
                statusOk && categoryOk
            }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun toggleCompleted(task: TodoTask) {
        scope.launch {
            val newCompleted = !task.completed
            repository.setCompleted(task.id, newCompleted)
            _refresh.value++
            val dep = TodoViewModel.dependencies
            if (newCompleted) {
                // Tarefa concluída: cancela o lembrete pendente.
                dep?.notifier?.cancel(task.id)
            } else {
                // Reaberta: reagenda o lembrete se ainda não venceu.
                task.dueDateTime?.let { due ->
                    if (due > currentTimeMillis()) {
                        dep?.notifier?.schedule(task.id, task.title, task.description, due)
                    }
                }
            }
        }
    }

    fun deleteTask(id: Long) {
        scope.launch {
            TodoViewModel.dependencies?.notifier?.cancel(id)
            repository.deleteTask(id)
            _refresh.value++
        }
    }

    companion object {
        // Definido pelo entry point de cada plataforma após createDependencies().
        var dependencies: AppDependencies? = null

        private val _globalRefresh = MutableStateFlow(0)

        /** Re-emite a lista de tarefas/categorias após escritas fora desta instância. */
        fun refreshNow() {
            _globalRefresh.value++
        }
    }
}
