package com.example.todo.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryUiModel(
    val id: Long,
    val name: String,
    val color: String,
    val taskCount: Long,
)

class CategoriesViewModel(private val repository: TodoRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _categories = MutableStateFlow<List<CategoryUiModel>>(emptyList())
    val categories: StateFlow<List<CategoryUiModel>> = _categories.asStateFlow()

    fun load() {
        scope.launch {
            val cats = repository.categoriesNow()
            _categories.value = cats.map {
                CategoryUiModel(it.id, it.name, it.color, repository.countTasksInCategory(it.id))
            }
        }
    }

    fun addCategory(name: String): Boolean {
        if (name.isBlank()) return false
        scope.launch {
            repository.insertCategory(name.trim(), "#607D8B")
            load()
            TodoViewModel.refreshNow()
        }
        return true
    }

    fun renameCategory(id: Long, newName: String): Boolean {
        if (newName.isBlank()) return false
        scope.launch {
            val cat = repository.categoryById(id)
            if (cat != null) {
                repository.updateCategory(id, newName.trim(), cat.color)
                load()
                TodoViewModel.refreshNow()
            }
        }
        return true
    }

    fun deleteCategory(id: Long) {
        scope.launch {
            repository.deleteCategory(id)
            load()
            TodoViewModel.refreshNow()
        }
    }
}
