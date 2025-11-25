package com.example.projectilumina.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectilumina.R
import com.example.projectilumina.data.Comentarios
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ComentarioAdapter(
    private val lista: List<Comentarios>
) : RecyclerView.Adapter<ComentarioAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foto = itemView.findViewById<ImageView>(R.id.comentario_profile)
        private val nome = itemView.findViewById<TextView>(R.id.comentario_nome)
        private val texto = itemView.findViewById<TextView>(R.id.comentario_texto)
        private val btnExcluir = itemView.findViewById<ImageButton>(R.id.btnExcluirComentario)

        fun bind(item: Comentarios) {
            nome.text = item.nomeUsuario
            texto.text = item.texto

            if (!item.fotoPerfilUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(item.fotoPerfilUrl)
                    .circleCrop()
                    .placeholder(R.drawable.icon_feed_profile)
                    .error(R.drawable.icon_feed_profile)
                    .into(foto)
            } else {
                foto.setImageResource(R.drawable.icon_feed_profile)
            }

            val uid = auth.currentUser?.uid
            btnExcluir.visibility = if (uid == item.userId) View.VISIBLE else View.GONE

            btnExcluir.setOnClickListener {
                excluirComentario(item)
            }
        }
    }

    private fun excluirComentario(item: Comentarios) {
        val feedId = item.feedId ?: return
        val comentarioId = item.comentarioId ?: return

        FirebaseDatabase.getInstance()
            .getReference("feed/$feedId/comentarios/$comentarioId")
            .removeValue()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comentario, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}
