package com.example.todo.shared.db

import com.example.todo.shared.currentTimeMillis

data class TodoTask(
    val id: Long,
    val title: String,
    val description: String,
    val completed: Boolean,
    val dueDateTime: Long?,
    val createdAt: Long,
    val categoryId: Long?,
) {
    val isOverdue: Boolean
        get() = dueDateTime != null && !completed && dueDateTime < currentTimeMillis()
}
