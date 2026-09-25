package com.example.todo.shared

import com.example.todo.shared.db.TodoTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskEditorState(
    val taskId: Long? = null,          // null = novo
    val title: String = "",
    val description: String = "",
    val hasDue: Boolean = false,
    val dueDateTime: Long? = null,
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val titleError: Boolean = false,
    val isNew: Boolean = true,
)

class TaskEditorViewModel(private val repository: TodoRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(TaskEditorState())
    val state: StateFlow<TaskEditorState> = _state.asStateFlow()

    fun load(taskId: Long?) {
        scope.launch {
            val cats = repository.categoriesNow()
            if (taskId == null) {
                _state.value = TaskEditorState(categories = cats, isNew = true)
            } else {
                val t = repository.taskById(taskId)
                _state.value = if (t == null) {
                    TaskEditorState(categories = cats, isNew = true)
                } else {
                    TaskEditorState(
                        taskId = t.id,
                        title = t.title,
                        description = t.description,
                        hasDue = t.dueDateTime != null,
                        dueDateTime = t.dueDateTime,
                        categoryId = t.categoryId,
                        categories = cats,
                        isNew = false,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, titleError = false)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun onHasDueChange(value: Boolean) {
        _state.value = _state.value.copy(hasDue = value, dueDateTime = if (value) _state.value.dueDateTime ?: currentTimeMillis() + 60_000L else null)
    }

    fun onDueChange(value: Long) {
        _state.value = _state.value.copy(dueDateTime = value)
    }

    fun onCategoryChange(value: Long?) {
        _state.value = _state.value.copy(categoryId = value)
    }

    /** Retorna true se salvou. Título vazio marca erro e não salva. */
    fun save(): Boolean {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = s.copy(titleError = true)
            return false
        }
        scope.launch {
            val due = if (s.hasDue) s.dueDateTime else null
            val dep = TodoViewModel.dependencies
            if (s.taskId == null) {
                val newId = repository.insertTask(s.title.trim(), s.description.trim(), due, s.categoryId)
                if (due != null && due > currentTimeMillis() && dep?.notifier?.isAvailable == true) {
                    dep.notifier.schedule(newId, s.title, s.description, due)
                }
            } else {
                val id = s.taskId
                val existing = repository.taskById(id)
                repository.updateTask(
                    TodoTask(
                        id = id,
                        title = s.title.trim(),
                        description = s.description.trim(),
                        completed = existing?.completed ?: false,
                        dueDateTime = due,
                        createdAt = existing?.createdAt ?: currentTimeMillis(),
                        categoryId = s.categoryId,
                    ),
                )
                if (dep != null) {
                    // Reagendar ou cancelar conforme a mudança de vencimento.
                    if (due != null && due > currentTimeMillis() && dep.notifier.isAvailable) {
                        dep.notifier.schedule(id, s.title, s.description, due)
                    } else {
                        dep.notifier.cancel(id)
                    }
                }
            }
            TodoViewModel.refreshNow()
        }
        return true
    }
}
