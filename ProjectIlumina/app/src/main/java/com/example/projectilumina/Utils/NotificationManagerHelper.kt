package com.example.projectilumina.Utils


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.projectilumina.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object NotificationManagerHelper {

    private const val CHANNEL_ID = "your_channel_id"
    private const val CHANNEL_NAME = "Notificações de Novas Mensagens"
    private const val CHANNEL_DESCRIPTION = "Canal de notificações para novas mensagens"

    fun verificarEEnviarNotificacao(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId)

        notificacoesRef.orderByChild("status").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        enviarNotificacaoLocal(context)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }


    private fun criarCanalDeNotificacao(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }


            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun enviarNotificacaoLocal(context: Context) {
        criarCanalDeNotificacao(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Você tem novas notificações!")
            .setContentText("Verifique suas notificações no aplicativo.")
            .setSmallIcon(R.drawable.icon_notificacao)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()


        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }
}
