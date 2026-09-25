package com.example.todo.shared

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.todo.shared.db.TodoDatabase

private lateinit var appContext: Context

class ContextHolder : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }
}

val appContextInstance: Context
    get() = if (::appContext.isInitialized) appContext else error("Application not started")

actual fun createDependencies(): AppDependencies {
    val driver: SqlDriver = AndroidSqliteDriver(TodoDatabase.Schema, appContextInstance, "todo.db")
    return AppDependencies(
        database = TodoDatabase(driver),
        notifier = AndroidTodoNotifier(appContextInstance),
    )
}
