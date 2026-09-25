package com.example.todo.shared

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

const val CHANNEL_ID = "todo_reminders"
const val EXTRA_TASK_ID = "task_id"
const val EXTRA_TITLE = "title"
const val EXTRA_BODY = "body"

class AndroidTodoNotifier(private val context: Context) : TodoNotifier {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes de tarefas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Notificações agendadas para tarefas com vencimento" }
        notificationManager.createNotificationChannel(channel)
    }

    override val isAvailable: Boolean
        get() = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    override fun schedule(taskId: Long, title: String, body: String, dueMillis: Long) {
        if (!isAvailable || dueMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Alarmes exatos (setExact*) exigem SCHEDULE_EXACT_ALARM concedida pelo usuário
        // em API 31+; sem ela, o sistema lança SecurityException. Tentamos o exact e,
        // se negado, caímos para o alarme inexact (janela de ~10 min) — nunca crashar.
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueMillis, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueMillis, pendingIntent)
        }
    }

    override fun cancel(taskId: Long) {
        val intent = Intent(context, TodoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Tarefa"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Checa permissão de novo (usuário pode ter revogado desde o agendamento).
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        nm.notify(taskId.toInt(), notification)
    }
}
