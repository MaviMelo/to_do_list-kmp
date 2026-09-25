package com.example.todo.shared

import com.example.todo.shared.db.TodoDatabase

// Cada plataforma fornece sua própria forma de construir o banco e as notificações.
// O resto do app é 100% comum.
class AppDependencies(
    val database: TodoDatabase,
    val notifier: TodoNotifier,
)

expect fun createDependencies(): AppDependencies
