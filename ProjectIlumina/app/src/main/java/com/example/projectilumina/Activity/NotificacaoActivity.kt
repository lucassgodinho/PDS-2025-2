package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
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
        carregarNotificacoes()
        setupNavigationButtons()

        NotificationUtils.atualizarIconeNotificacao(binding.appBarDefault.root)
    }

    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Notificação"

        binding.appBarDefault.iconReturn.setOnClickListener {
            finish()
        }

        binding.endBar.iconDenuncia.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        binding.endBar.iconHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        notificacoesAdapter = NotificacaoAdapter(notificacoesList)
        binding.recyclerViewNotificacoes.apply {
            layoutManager = LinearLayoutManager(this@NotificacaoActivity)
            adapter = notificacoesAdapter
        }
    }

    private fun carregarNotificacoes() {
        notificacoesList.clear()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId)


        notificacoesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificacoesList.clear()
                for (notificacaoSnapshot in snapshot.children) {
                    val notificacao = notificacaoSnapshot.getValue(Notificacao::class.java)
                    notificacao?.let { notificacoesList.add(it) }
                }
                notificacoesAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificacaoActivity, "Erro ao carregar notificações", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupChildEventListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId)

        if (!isChildListenerAttached) {
            childEventListener = notificacoesRef.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val notificacao = snapshot.getValue(Notificacao::class.java)
                    notificacao?.let {
                        if (notificacoesList.none { it.denunciaId == notificacao.denunciaId }) {
                            notificacoesList.add(it)
                            notificacoesAdapter.notifyItemInserted(notificacoesList.size - 1)
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val notificacao = snapshot.getValue(Notificacao::class.java)
                    notificacao?.let {
                        val index = notificacoesList.indexOfFirst { it.denunciaId == notificacao.denunciaId }
                        if (index != -1) {
                            notificacoesList[index] = notificacao
                            notificacoesAdapter.notifyItemChanged(index)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val notificacao = snapshot.getValue(Notificacao::class.java)
                    notificacao?.let {
                        notificacoesList.remove(it)
                        notificacoesAdapter.notifyDataSetChanged()
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NotificacaoActivity, "Erro ao carregar notificações", Toast.LENGTH_SHORT).show()
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
        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId)


        childEventListener?.let { notificacoesRef.removeEventListener(it) }
        isChildListenerAttached = false
    }

    override fun onResume() {
        super.onResume()
        marcarNotificacoesComoLidas()
    }

    private fun marcarNotificacoesComoLidas() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId)

        notificacoesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (notificacaoSnapshot in snapshot.children) {
                    notificacaoSnapshot.ref.child("status").setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificacaoActivity, "Erro ao marcar notificações como lidas", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
