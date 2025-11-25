package com.example.projectilumina.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectilumina.Activity.CommentsActivity
import com.example.projectilumina.R
import com.example.projectilumina.data.Comentarios
import com.example.projectilumina.data.FeedItem
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FeedAdapter(
    private val feedList: List<FeedItem>,
    private val onItemClick: (FeedItem) -> Unit,
    private val onSendComment: (FeedItem, String) -> Unit
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val feedRef = FirebaseDatabase.getInstance().getReference("feed")

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageProfile = itemView.findViewById<ImageView>(R.id.image_profile)
        private val nameFeed = itemView.findViewById<TextView>(R.id.name_feed)

        private val imageFeed = itemView.findViewById<ShapeableImageView>(R.id.image_feed)


        private val tvDescricao = itemView.findViewById<TextView>(R.id.tvDescricao)
        private val tvProblema = itemView.findViewById<TextView>(R.id.tvProblema)
        private val iconLike = itemView.findViewById<ImageView>(R.id.icon_like)
        private val textLikes = itemView.findViewById<TextView>(R.id.text_like_count)


        private val edtComentario = itemView.findViewById<EditText>(R.id.edtComentario)
        private val btnEnviar = itemView.findViewById<ImageButton>(R.id.btnEnviar)

        private val previewComentarios =
            itemView.findViewById<LinearLayout>(R.id.previewComentarios)
        private val btnVerTodos =
            itemView.findViewById<TextView>(R.id.btnVerTodosComentarios)

        fun bind(item: FeedItem) {

            val nome = item.nomeUsuario ?: "Usuário"
            val cidade = item.cidade
            nameFeed.text = if (!cidade.isNullOrBlank()) "$nome, $cidade" else nome


            if (!item.fotoPerfilUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(item.fotoPerfilUrl)
                    .circleCrop()
                    .placeholder(R.drawable.icon_feed_profile)
                    .error(R.drawable.icon_feed_profile)
                    .into(imageProfile)
            } else {
                imageProfile.setImageResource(R.drawable.icon_feed_profile)
            }

            if (!item.imagemUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(item.imagemUrl)
                    .placeholder(R.drawable.poste_sem_iluminacao)
                    .error(R.drawable.poste_sem_iluminacao)
                    .into(imageFeed)
            } else {
                imageFeed.setImageResource(R.drawable.poste_sem_iluminacao)
            }

            val problema = item.problema ?: ""
            tvProblema.text = "Problema: $problema"


            val descricao = item.descricao ?: ""
            tvDescricao.text = "Descrição: $descricao"

            val uid = auth.currentUser?.uid
            val jaCurtiu = uid != null && (item.likes?.get(uid) == true)

            atualizarUILike(item, jaCurtiu)


            iconLike.setOnClickListener {
                if (uid != null && item.id != null) {
                    alternarLike(item, uid)
                }
            }


            itemView.setOnClickListener {
                onItemClick(item)
            }


            btnEnviar.setOnClickListener {
                val texto = edtComentario.text.toString().trim()
                if (texto.isNotEmpty()) {
                    onSendComment(item, texto)
                    edtComentario.setText("")
                }
            }

            carregarPreviewComentarios(item)
        }

        /** Atualiza só a parte visual do like (ícone + texto) */
        private fun atualizarUILike(item: FeedItem, jaCurtiu: Boolean) {
            if (jaCurtiu) {
                iconLike.setImageResource(R.drawable.icon_like_on)
            } else {
                iconLike.setImageResource(R.drawable.icon_like_off)
            }

            val qtd = item.curtidas
            textLikes.text = if (qtd == 1) "1 like" else "$qtd likes"
        }

        /** Alterna like/deslike e atualiza Firebase + objeto item */
        private fun alternarLike(item: FeedItem, uid: String) {
            val feedId = item.id ?: return

            val likesUserRef = feedRef.child(feedId).child("likes").child(uid)
            val curtidasRef = feedRef.child(feedId).child("curtidas")

            likesUserRef.get().addOnSuccessListener { snapshot ->
                val jaCurtiuAntes = snapshot.exists()

                if (jaCurtiuAntes) {
                    likesUserRef.removeValue()

                    val novaQtd = (item.curtidas - 1).coerceAtLeast(0)
                    item.curtidas = novaQtd
                    item.likes?.remove(uid)

                    curtidasRef.setValue(novaQtd)
                    atualizarUILike(item, false)
                } else {
                    likesUserRef.setValue(true)

                    val novaQtd = item.curtidas + 1
                    item.curtidas = novaQtd
                    if (item.likes == null) {
                        item.likes = mutableMapOf()
                    }
                    item.likes!![uid] = true

                    curtidasRef.setValue(novaQtd)
                    atualizarUILike(item, true)
                }
            }
        }

        private fun carregarPreviewComentarios(item: FeedItem) {
            val feedId = item.id ?: return

            val ref = FirebaseDatabase.getInstance()
                .getReference("feed/$feedId/comentarios")

            ref.limitToLast(2).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    previewComentarios.removeAllViews()

                    val lista = ArrayList<Comentarios>()
                    for (ds in snapshot.children) {
                        val c = ds.getValue(Comentarios::class.java)
                        if (c != null && c.deletada != true) {
                            lista.add(c)
                        }
                    }

                    for (c in lista) {
                        val tv = TextView(itemView.context)
                        tv.text = "${c.nomeUsuario}: ${c.texto}"
                        tv.textSize = 14f
                        previewComentarios.addView(tv)
                    }

                    btnVerTodos.visibility =
                        if (lista.isNotEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            btnVerTodos.setOnClickListener {
                val intent = Intent(itemView.context, CommentsActivity::class.java)
                intent.putExtra("FEED_ID", item.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(feedList[position])
    }

    override fun getItemCount(): Int = feedList.size
}
