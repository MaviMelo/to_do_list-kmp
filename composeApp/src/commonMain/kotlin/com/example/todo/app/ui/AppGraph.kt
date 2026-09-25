package com.example.todo.app.ui

import com.example.todo.shared.AppDependencies
import com.example.todo.shared.TodoRepository
import com.example.todo.shared.TodoViewModel

/**
 * Grafo simples de dependências, populado pelo entry point de cada plataforma
 * (Android Application / desktop main). A UI comum consome apenas isto.
 */
object AppGraph {
    lateinit var repository: TodoRepository

    fun init(dependencies: AppDependencies) {
        repository = TodoRepository(dependencies.database)
        TodoViewModel.dependencies = dependencies
    }

    fun taskDeleter(taskId: Long?) {
        if (taskId != null) {
            TodoViewModel.dependencies?.notifier?.cancel(taskId)
            // Delegado ao repositório por meio do ViewModel de lista; aqui usamos
            // direto o repositório para não depender de escopo de coroutine da UI.
            repository.deleteTask(taskId)
        }
    }
}
