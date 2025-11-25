package com.example.projectilumina.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectilumina.Adapter.ComentarioAdapter
import com.example.projectilumina.data.Comentarios
import com.example.projectilumina.databinding.ActivityCommentsBinding
import com.google.firebase.database.*

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var adapter: ComentarioAdapter
    private val lista = ArrayList<Comentarios>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val feedId = intent.getStringExtra("FEED_ID") ?: return

        adapter = ComentarioAdapter(lista)
        binding.recyclerComentarios.layoutManager = LinearLayoutManager(this)
        binding.recyclerComentarios.adapter = adapter
        binding.iconReturn.setOnClickListener {
            finish()
        }


        carregarComentarios(feedId)
    }

    private fun carregarComentarios(feedId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("feed/$feedId/comentarios")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lista.clear()
                for (ds in snapshot.children) {
                    val c = ds.getValue(Comentarios::class.java)
                    if (c != null) lista.add(c)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
