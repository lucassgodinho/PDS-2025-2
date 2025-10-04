package com.example.projectilumina.Utils

import android.view.View
import android.widget.ImageView
import com.example.projectilumina.R
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


object NotificationUtils {

    fun atualizarIconeNotificacao(appBar: View) {
        val notificationIcon: ImageView = when (appBar) {
            is AppBarLayout -> appBar.findViewById(R.id.icon_notificacao)
            else -> appBar.findViewById(R.id.icon_notificacao)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId)

        notificacoesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var temNotificacaoNova = false

                for (notificacaoSnapshot in snapshot.children) {
                    val status = notificacaoSnapshot.child("status").getValue(Boolean::class.java) ?: true
                    if (!status) {
                        temNotificacaoNova = true
                        break
                    }
                }

                if (temNotificacaoNova) {
                    notificationIcon.setImageResource(R.drawable.icon_notificacao_ativa)
                } else {
                    notificationIcon.setImageResource(R.drawable.icon_notificacao)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Erro ao monitorar notificações: ${error.message}")
            }
        })
    }
}
