package com.jvf.monitorfunctional.service
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jvf.monitorfunctional.MainActivity
import android.util.Log

class AlertaNotificationService : FirebaseMessagingService(){

    override fun onNewToken(token: String){
        super.onNewToken(token)
        Log.d("Aware Kids", "Novo token recebido do Firebase: $token")
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            mostrarNotificacao(it.title ?: "Alerta", it.body ?: "Novo vídeo inadequado detectado.")
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun mostrarNotificacao(titulo: String, mensagem: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "alertas_monitor"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Segurança",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        // Exibe a notificação
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
