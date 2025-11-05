package com.example.projectilumina.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectilumina.R
import com.example.projectilumina.data.Notificacao

class NotificacaoAdapter(private val notificacoes: List<Notificacao>) :
    RecyclerView.Adapter<NotificacaoAdapter.NotificacaoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacaoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificacao, parent, false)
        return NotificacaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacaoViewHolder, position: Int) {
        val notificacao = notificacoes[position]
        holder.bind(notificacao)
    }

    override fun getItemCount() = notificacoes.size

    class NotificacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dataTextView: TextView = itemView.findViewById(R.id.data)
        private val tipoManutencaoTextView: TextView = itemView.findViewById(R.id.tvTipoManutencao)
        private val descricaoTextView: TextView = itemView.findViewById(R.id.tvDescricao)

        fun bind(notificacao: Notificacao) {
            dataTextView.text ="Data: ${notificacao.data}"
            tipoManutencaoTextView.text ="Tipo da Manutenção: ${notificacao.tipoManutencao}"
            descricaoTextView.text = "Descrição: ${notificacao.descricao}"
        }
    }
}