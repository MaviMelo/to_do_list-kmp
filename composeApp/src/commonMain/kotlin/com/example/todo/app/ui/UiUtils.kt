package com.example.todo.app.ui

import androidx.compose.ui.graphics.Color
import com.example.todo.shared.currentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun parseColor(hex: String): Color = try {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    when (clean.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> Color(0xFF607D8B)
    }
} catch (e: Exception) {
    Color(0xFF607D8B)
}

enum class DueUrgency { NONE, SOON, OVERDUE }

/** Classifica o vencimento para dar destaque visual na lista. */
fun dueUrgency(epochMillis: Long, completed: Boolean): DueUrgency {
    if (completed) return DueUrgency.NONE
    val diff = epochMillis - currentTimeMillis()
    return when {
        diff < 0 -> DueUrgency.OVERDUE
        diff <= 24 * 3600 * 1000L -> DueUrgency.SOON
        else -> DueUrgency.NONE
    }
}

fun formatDue(epochMillis: Long, completed: Boolean): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val dateStr = "%02d/%02d %02d:%02d".format(local.dayOfMonth, local.monthNumber, local.hour, local.minute)
    return when (dueUrgency(epochMillis, completed)) {
        DueUrgency.OVERDUE -> "Venceu: $dateStr"
        DueUrgency.SOON -> "Vence em breve: $dateStr"
        DueUrgency.NONE -> "Vence: $dateStr"
    }
}
