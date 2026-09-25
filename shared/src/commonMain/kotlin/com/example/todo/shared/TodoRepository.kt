package com.example.todo.shared

import app.cash.sqldelight.coroutines.asFlow
import com.example.todo.shared.db.TodoDatabase
import com.example.todo.shared.db.TodoTask
import com.example.todo.shared.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoRepository(private val database: TodoDatabase) {

    // ---------- Tarefas ----------

    fun tasks(): Flow<List<TodoTask>> =
        database.todoQueries.selectAllTasks().asFlow().map { it.executeAsList().toTodoTasks() }

    fun taskById(id: Long): TodoTask? =
        database.todoQueries.selectTaskById(id).executeAsOneOrNull()?.toTodoTask()

    fun insertTask(
        title: String,
        description: String,
        dueDateTime: Long?,
        categoryId: Long?,
    ): Long {
        val id = kotlin.random.Random.nextLong(1_000_000_000L, 9_999_999_999L)
        database.todoQueries.insertTaskWithId(
            id = id,
            title = title,
            description = description,
            completed = 0L,
            dueDateTime = dueDateTime,
            createdAt = currentTimeMillis(),
            categoryId = categoryId,
        )
        return id
    }

    fun updateTask(task: TodoTask) {
        database.todoQueries.updateTask(
            id = task.id,
            title = task.title,
            description = task.description,
            completed = if (task.completed) 1L else 0L,
            dueDateTime = task.dueDateTime,
            categoryId = task.categoryId,
        )
    }

    fun setCompleted(id: Long, completed: Boolean) {
        // Argumentos nomeados: a ordem gerada pelo SQLDelight é (completed, id).
        database.todoQueries.setTaskCompleted(completed = if (completed) 1L else 0L, id = id)
    }

    fun deleteTask(id: Long) {
        database.todoQueries.deleteTask(id)
    }

    // ---------- Categorias ----------

    fun categories(): Flow<List<Category>> =
        database.todoQueries.selectAllCategories().asFlow().map { query ->
            query.executeAsList().map { Category(it.id, it.name, it.color) }
        }

    fun categoryById(id: Long): Category? =
        database.todoQueries.selectCategoryById(id).executeAsOneOrNull()
            ?.let { Category(it.id, it.name, it.color) }

    /** Lista síncrona de categorias (para uso pontual em ViewModels). */
    fun categoriesNow(): List<Category> =
        database.todoQueries.selectAllCategories().executeAsList()
            .map { Category(it.id, it.name, it.color) }

    fun insertCategory(name: String, color: String) {
        database.todoQueries.insertCategory(name = name, color = color)
    }

    fun updateCategory(id: Long, name: String, color: String) {
        database.todoQueries.updateCategory(id = id, name = name, color = color)
    }

    fun deleteCategory(id: Long) {
        // ON DELETE SET NULL garante que as tarefas ficam, sem categoria.
        database.todoQueries.deleteCategory(id)
    }

    fun countTasksInCategory(categoryId: Long): Long =
        database.todoQueries.countTasksInCategory(categoryId).executeAsOne()
}

private fun com.example.todo.shared.db.Task.toTodoTask() = TodoTask(
    id = id,
    title = title,
    description = description,
    completed = completed == 1L,
    dueDateTime = dueDateTime,
    createdAt = createdAt,
    categoryId = categoryId,
)

private fun List<com.example.todo.shared.db.Task>.toTodoTasks() = map { it.toTodoTask() }
