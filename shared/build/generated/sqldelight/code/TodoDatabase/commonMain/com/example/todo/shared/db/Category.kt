package com.example.todo.shared.db

import kotlin.Long
import kotlin.String

public data class Category(
  public val id: Long,
  public val name: String,
  public val color: String,
)
