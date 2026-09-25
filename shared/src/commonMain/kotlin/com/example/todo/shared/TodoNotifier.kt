package com.example.todo.shared

import kotlinx.coroutines.flow.Flow

/**
 * Abstração para notificações locais agendadas.
 * Android: AlarmManager + BroadcastReceiver + NotificationManager.
 * JVM/desktop: no-op (registra em log).
 */
interface TodoNotifier {
    /** Agenda notificação para a tarefa em [dueMillis] (epoch millis). */
    fun schedule(taskId: Long, title: String, body: String, dueMillis: Long)

    /** Cancela a notificação da tarefa. */
    fun cancel(taskId: Long)

    /** Indica se notificações estão disponíveis (permissão concedida). */
    val isAvailable: Boolean

    /** Fluxo (opcional) para a UI saber o status da permissão. */
    val permissionEvents: Flow<Boolean> get() = kotlinx.coroutines.flow.emptyFlow()
}
