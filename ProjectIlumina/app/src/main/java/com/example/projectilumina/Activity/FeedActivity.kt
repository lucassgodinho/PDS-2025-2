package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectilumina.Adapter.FeedAdapter
import com.example.projectilumina.R
import com.example.projectilumina.data.Comentarios
import com.example.projectilumina.data.FeedItem
import com.example.projectilumina.databinding.ActivityFeedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var adapter: FeedAdapter
    private val feedList = ArrayList<FeedItem>()

    private lateinit var feedRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerFeed.layoutManager = LinearLayoutManager(this)

        auth = FirebaseAuth.getInstance()
        feedRef = FirebaseDatabase.getInstance().getReference("feed")
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        adapter = FeedAdapter(
            feedList,
            onItemClick = {  },
            onSendComment = { item, texto ->
                salvarComentario(item, texto)
            }
        )

        binding.recyclerFeed.adapter = adapter

        setupNavigationButtons()
        updateIconColors()
        carregarFeed()
    }

    private fun updateIconColors() {

        binding.endBar.iconFeed.setColorFilter(
            ContextCompat.getColor(this, R.color.color_app_bar)
        )
        binding.endBar.iconDenuncia.setColorFilter(ContextCompat.getColor(this, R.color.black))
        binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.black))
    }

    private fun setupNavigationButtons() {

        binding.appBarHome.textActivity.text = "Feed"

        binding.appBarHome.iconNotificacao.setOnClickListener {
            startActivity(Intent(this, NotificacaoActivity::class.java))
        }

        binding.appBarHome.iconMenu.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.endBar.iconMapa.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        binding.endBar.iconDenuncia.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.endBar.iconFeed.setOnClickListener {
        }
    }

    private fun carregarFeed() {
        feedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                feedList.clear()

                for (ds in snapshot.children) {
                    val item = ds.getValue(FeedItem::class.java)
                    if (item != null) {
                        item.id = ds.key
                        feedList.add(item)
                    }
                }

                feedList.reverse()
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@FeedActivity,
                    "Erro ao carregar o feed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun salvarComentario(feedItem: FeedItem, texto: String) {
        val uid = auth.currentUser?.uid ?: return

        usersRef.child(uid).get().addOnSuccessListener { snapshot ->

            val nome = snapshot.child("nome").getValue(String::class.java) ?: "Usuário"
            val fotoPerfilUrl = snapshot.child("imgperfil").getValue(String::class.java)

            val comentariosRef = feedRef.child(feedItem.id!!).child("comentarios")

            val novoComentarioRef = comentariosRef.push()
            val comentarioId = novoComentarioRef.key ?: return@addOnSuccessListener

            val dataHora = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date())

            val comentario = Comentarios(
                comentarioId = comentarioId,
                feedId = feedItem.id,
                userId = uid,
                nomeUsuario = nome,
                fotoPerfilUrl = fotoPerfilUrl,
                texto = texto,
                deletada = false,
                dataHora = dataHora
            )

            novoComentarioRef.setValue(comentario)
        }
    }
}
