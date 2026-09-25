package com.example.todo.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.todo.shared.db.TodoDatabase
import java.io.File

actual fun createDependencies(): AppDependencies {
    val dbFile = File(System.getProperty("user.home"), ".todo-kmp/todo.db")
    dbFile.parentFile?.mkdirs()
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    // Cria o schema se o banco acabou de ser criado (arquivo vazio).
    // JdbcSqliteDriver com driver embutido já executa PRAGMA; schema manual aqui.
    if (dbFile.length() == 0L) {
        TodoDatabase.Schema.create(driver)
    }
    return AppDependencies(
        database = TodoDatabase(driver),
        notifier = DesktopTodoNotifier(),
    )
}

class DesktopTodoNotifier : TodoNotifier {
    override fun schedule(taskId: Long, title: String, body: String, dueMillis: Long) {
        println("[notify] agendada (desktop no-op): #$taskId '$title' '$body' @ $dueMillis")
    }

    override fun cancel(taskId: Long) {
        println("[notify] cancelada (desktop no-op): #$taskId")
    }

    override val isAvailable: Boolean = false
}
