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
            // Primeira cor livre da paleta evita que todas as novas compartilhem a mesma cor.
            val used = repository.categoriesNow().map { it.color }.toSet()
            val color = CATEGORY_COLORS.firstOrNull { it !in used }
                ?: CATEGORY_COLORS[repository.categoriesNow().size % CATEGORY_COLORS.size]
            repository.insertCategory(name.trim(), color)
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

    private companion object {
        // Cores Material distintas; primeira não usada é escolhida ao criar categoria.
        val CATEGORY_COLORS = listOf(
            "#F44336", // vermelho
            "#E91E63", // rosa
            "#9C27B0", // roxo
            "#3F51B5", // índigo
            "#2196F3", // azul
            "#009688", // teal
            "#4CAF50", // verde
            "#FF9800", // laranja
            "#795548", // marrom
            "#607D8B", // blue grey
        )
    }
}
