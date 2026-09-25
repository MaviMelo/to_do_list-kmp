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

    val tasks: StateFlow<List<TaskWithCategory>> = combine(
        repository.tasks(),
        repository.categories(),
        _filter,
    ) { tasks, cats, filter ->
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
            repository.setCompleted(task.id, !task.completed)
            val dep = TodoViewModel.dependencies
            if (!task.completed) {
                task.dueDateTime?.let { due ->
                    if (due > currentTimeMillis()) {
                        dep?.notifier?.schedule(task.id, task.title, task.description, due)
                    }
                }
            } else {
                dep?.notifier?.cancel(task.id)
            }
        }
    }

    fun deleteTask(id: Long) {
        scope.launch {
            TodoViewModel.dependencies?.notifier?.cancel(id)
            repository.deleteTask(id)
        }
    }

    companion object {
        // Definido pelo entry point de cada plataforma após createDependencies().
        var dependencies: AppDependencies? = null
    }
}
