package com.example.todo.shared.db

import kotlin.Long
import kotlin.String

public data class Task(
  public val id: Long,
  public val title: String,
  public val description: String,
  public val completed: Long,
  public val dueDateTime: Long?,
  public val createdAt: Long,
  public val categoryId: Long?,
)
