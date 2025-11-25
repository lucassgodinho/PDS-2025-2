package com.example.projectilumina.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.projectilumina.R
import com.example.projectilumina.data.Notificacao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val problema = remoteMessage.data["problema"]
            ?: remoteMessage.notification?.title
            ?: "Nova denúncia"

        val descricao = remoteMessage.data["descricao"]
            ?: remoteMessage.notification?.body
            ?: "Abra o app para ver detalhes"

        val denunciaId = remoteMessage.data["denunciaId"] ?: ""

        val autorId = remoteMessage.data["autorId"] ?: ""
        val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (autorId == meuUid) {
            return
        }

        val dataFormatada = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val notificacao = Notificacao(
            denunciaId = denunciaId,
            data = dataFormatada,
            problema = problema,
            descricao = descricao,
            status = false
        )

        salvarNotificacaoNoFirebase(notificacao)

        val titulo = "Problema: $problema"
        mostrarNotificacao(titulo, descricao)
    }





    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .child("token")
            .setValue(token)
    }

    private fun mostrarNotificacao(titulo: String, mensagem: String) {
        val channelId = "denuncias_proximas"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Denúncias próximas",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon_mapa_pendente)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    private fun salvarNotificacaoNoFirebase(notificacao: Notificacao) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val denunciaId = notificacao.denunciaId ?: return

        FirebaseDatabase.getInstance()
            .getReference("notificacoes")
            .child(uid)
            .child(denunciaId)
            .setValue(notificacao)
    }



}

