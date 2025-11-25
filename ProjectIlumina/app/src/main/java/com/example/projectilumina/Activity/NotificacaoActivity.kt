package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectilumina.Adapter.NotificacaoAdapter
import com.example.projectilumina.Utils.NotificationUtils
import com.example.projectilumina.data.Notificacao
import com.example.projectilumina.databinding.ActivityNotificacaoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificacaoActivity : AppCompatActivity() {

    private lateinit var notificacoesAdapter: NotificacaoAdapter
    private val notificacoesList = mutableListOf<Notificacao>()
    private lateinit var binding: ActivityNotificacaoBinding

    private var childEventListener: ChildEventListener? = null
    private var isChildListenerAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificacaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupNavigationButtons()

        NotificationUtils.atualizarIconeNotificacao(binding.appBarDefault.root)
    }

    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Notificação"

        binding.appBarDefault.iconReturn.setOnClickListener { finish() }

        binding.endBar.iconDenuncia.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.endBar.iconMapa.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        binding.endBar.iconFeed.setOnClickListener {
            startActivity(Intent(this, FeedActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        notificacoesAdapter = NotificacaoAdapter(notificacoesList)
        binding.recyclerViewNotificacoes.apply {
            layoutManager = LinearLayoutManager(this@NotificacaoActivity)
            adapter = notificacoesAdapter
        }
    }

    private fun setupChildEventListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance()
            .getReference("notificacoes")
            .child(userId)

        if (!isChildListenerAttached) {
            notificacoesList.clear()
            notificacoesAdapter.notifyDataSetChanged()

            childEventListener = notificacoesRef.addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val notificacao = snapshot.getValue(Notificacao::class.java) ?: return

                    val index = notificacoesList.indexOfFirst {
                        it.denunciaId == notificacao.denunciaId
                    }

                    if (index == -1) {

                        notificacoesList.add(notificacao)
                        notificacoesAdapter.notifyItemInserted(notificacoesList.size - 1)
                    } else {

                        notificacoesList[index] = notificacao
                        notificacoesAdapter.notifyItemChanged(index)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val notificacao = snapshot.getValue(Notificacao::class.java) ?: return

                    val index = notificacoesList.indexOfFirst {
                        it.denunciaId == notificacao.denunciaId
                    }

                    if (index != -1) {
                        notificacoesList[index] = notificacao
                        notificacoesAdapter.notifyItemChanged(index)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val notificacao = snapshot.getValue(Notificacao::class.java) ?: return

                    val index = notificacoesList.indexOfFirst {
                        it.denunciaId == notificacao.denunciaId
                    }

                    if (index != -1) {
                        notificacoesList.removeAt(index)
                        notificacoesAdapter.notifyItemRemoved(index)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@NotificacaoActivity,
                        "Erro ao carregar notificações",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("NotificacaoActivity", error.message)
                }
            })

            isChildListenerAttached = true
        }
    }

    override fun onStart() {
        super.onStart()
        setupChildEventListener()
    }

    override fun onStop() {
        super.onStop()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance()
            .getReference("notificacoes")
            .child(userId)

        childEventListener?.let { notificacoesRef.removeEventListener(it) }
        isChildListenerAttached = false
    }

    override fun onResume() {
        super.onResume()
        marcarNotificacoesComoLidas()
    }

    private fun marcarNotificacoesComoLidas() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance()
            .getReference("notificacoes")
            .child(userId)

        notificacoesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (notificacaoSnapshot in snapshot.children) {
                    notificacaoSnapshot.ref.child("status").setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@NotificacaoActivity,
                    "Erro ao marcar notificações como lidas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}




