package com.example.todo.shared.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class TodoQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllTasks(mapper: (
    id: Long,
    title: String,
    description: String,
    completed: Long,
    dueDateTime: Long?,
    createdAt: Long,
    categoryId: Long?,
  ) -> T): Query<T> = Query(-1_566_310_329, arrayOf("Task"), driver, "Todo.sq", "selectAllTasks",
      "SELECT Task.id, Task.title, Task.description, Task.completed, Task.dueDateTime, Task.createdAt, Task.categoryId FROM Task ORDER BY completed ASC, CASE WHEN dueDateTime IS NULL THEN 1 ELSE 0 END, dueDateTime ASC, createdAt DESC") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4),
      cursor.getLong(5)!!,
      cursor.getLong(6)
    )
  }

  public fun selectAllTasks(): Query<Task> = selectAllTasks { id, title, description, completed,
      dueDateTime, createdAt, categoryId ->
    Task(
      id,
      title,
      description,
      completed,
      dueDateTime,
      createdAt,
      categoryId
    )
  }

  public fun <T : Any> selectTaskById(id: Long, mapper: (
    id: Long,
    title: String,
    description: String,
    completed: Long,
    dueDateTime: Long?,
    createdAt: Long,
    categoryId: Long?,
  ) -> T): Query<T> = SelectTaskByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4),
      cursor.getLong(5)!!,
      cursor.getLong(6)
    )
  }

  public fun selectTaskById(id: Long): Query<Task> = selectTaskById(id) { id_, title, description,
      completed, dueDateTime, createdAt, categoryId ->
    Task(
      id_,
      title,
      description,
      completed,
      dueDateTime,
      createdAt,
      categoryId
    )
  }

  public fun <T : Any> selectAllCategories(mapper: (
    id: Long,
    name: String,
    color: String,
  ) -> T): Query<T> = Query(-1_065_956_093, arrayOf("Category"), driver, "Todo.sq",
      "selectAllCategories",
      "SELECT Category.id, Category.name, Category.color FROM Category ORDER BY name ASC") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!
    )
  }

  public fun selectAllCategories(): Query<Category> = selectAllCategories { id, name, color ->
    Category(
      id,
      name,
      color
    )
  }

  public fun <T : Any> selectCategoryById(id: Long, mapper: (
    id: Long,
    name: String,
    color: String,
  ) -> T): Query<T> = SelectCategoryByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!
    )
  }

  public fun selectCategoryById(id: Long): Query<Category> = selectCategoryById(id) { id_, name,
      color ->
    Category(
      id_,
      name,
      color
    )
  }

  public fun countTasksInCategory(categoryId: Long?): Query<Long> =
      CountTasksInCategoryQuery(categoryId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertTask(
    title: String,
    description: String,
    completed: Long,
    dueDateTime: Long?,
    createdAt: Long,
    categoryId: Long?,
  ) {
    driver.execute(-1_026_577_508, """
        |INSERT INTO Task (title, description, completed, dueDateTime, createdAt, categoryId)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, completed)
          bindLong(3, dueDateTime)
          bindLong(4, createdAt)
          bindLong(5, categoryId)
        }
    notifyQueries(-1_026_577_508) { emit ->
      emit("Task")
    }
  }

  public fun updateTask(
    title: String,
    description: String,
    completed: Long,
    dueDateTime: Long?,
    categoryId: Long?,
    id: Long,
  ) {
    driver.execute(2_006_292_908, """
        |UPDATE Task SET title = ?, description = ?, completed = ?,
        |    dueDateTime = ?, categoryId = ? WHERE id = ?
        """.trimMargin(), 6) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, completed)
          bindLong(3, dueDateTime)
          bindLong(4, categoryId)
          bindLong(5, id)
        }
    notifyQueries(2_006_292_908) { emit ->
      emit("Task")
    }
  }

  public fun setTaskCompleted(completed: Long, id: Long) {
    driver.execute(-301_898_174, """UPDATE Task SET completed = ? WHERE id = ?""", 2) {
          bindLong(0, completed)
          bindLong(1, id)
        }
    notifyQueries(-301_898_174) { emit ->
      emit("Task")
    }
  }

  public fun deleteTask(id: Long) {
    driver.execute(-228_153_970, """DELETE FROM Task WHERE id = ?""", 1) {
          bindLong(0, id)
        }
    notifyQueries(-228_153_970) { emit ->
      emit("Task")
    }
  }

  public fun insertCategory(name: String, color: String) {
    driver.execute(1_362_448_277, """INSERT INTO Category (name, color) VALUES (?, ?)""", 2) {
          bindString(0, name)
          bindString(1, color)
        }
    notifyQueries(1_362_448_277) { emit ->
      emit("Category")
    }
  }

  public fun updateCategory(
    name: String,
    color: String,
    id: Long,
  ) {
    driver.execute(909_489_573, """UPDATE Category SET name = ?, color = ? WHERE id = ?""", 3) {
          bindString(0, name)
          bindString(1, color)
          bindLong(2, id)
        }
    notifyQueries(909_489_573) { emit ->
      emit("Category")
    }
  }

  public fun deleteCategory(id: Long) {
    driver.execute(-2_013_659_001, """DELETE FROM Category WHERE id = ?""", 1) {
          bindLong(0, id)
        }
    notifyQueries(-2_013_659_001) { emit ->
      emit("Category")
      emit("Task")
    }
  }

  private inner class SelectTaskByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Task", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Task", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(530_435_281,
        """SELECT Task.id, Task.title, Task.description, Task.completed, Task.dueDateTime, Task.createdAt, Task.categoryId FROM Task WHERE id = ?""",
        mapper, 1) {
      bindLong(0, id)
    }

    override fun toString(): String = "Todo.sq:selectTaskById"
  }

  private inner class SelectCategoryByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Category", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Category", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_130_218_294,
        """SELECT Category.id, Category.name, Category.color FROM Category WHERE id = ?""", mapper,
        1) {
      bindLong(0, id)
    }

    override fun toString(): String = "Todo.sq:selectCategoryById"
  }

  private inner class CountTasksInCategoryQuery<out T : Any>(
    public val categoryId: Long?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Task", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Task", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT COUNT(*) FROM Task WHERE categoryId ${ if (categoryId == null) "IS" else "=" } ?""",
        mapper, 1) {
      bindLong(0, categoryId)
    }

    override fun toString(): String = "Todo.sq:countTasksInCategory"
  }
}
