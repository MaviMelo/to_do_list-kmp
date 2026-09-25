package com.example.todo.shared.db.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.example.todo.shared.db.TodoDatabase
import com.example.todo.shared.db.TodoQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<TodoDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = TodoDatabaseImpl.Schema

internal fun KClass<TodoDatabase>.newInstance(driver: SqlDriver): TodoDatabase =
    TodoDatabaseImpl(driver)

private class TodoDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), TodoDatabase {
  override val todoQueries: TodoQueries = TodoQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE Task (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL DEFAULT '',
          |    completed INTEGER NOT NULL DEFAULT 0,
          |    dueDateTime INTEGER,              -- epoch millis; NULL = sem vencimento
          |    createdAt INTEGER NOT NULL,
          |    categoryId INTEGER,               -- NULL = sem categoria
          |    FOREIGN KEY (categoryId) REFERENCES Category(id) ON DELETE SET NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Category (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL,
          |    color TEXT NOT NULL DEFAULT '#FF5722'
          |)
          """.trimMargin(), 0)
      driver.execute(null, "INSERT INTO Category (name, color) VALUES ('Pessoal', '#2196F3')", 0)
      driver.execute(null, "INSERT INTO Category (name, color) VALUES ('Trabalho', '#4CAF50')", 0)
      driver.execute(null, "INSERT INTO Category (name, color) VALUES ('Estudos', '#FF9800')", 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
