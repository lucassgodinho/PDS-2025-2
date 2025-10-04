package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectilumina.Adapter.ProfileAdapter
import com.example.projectilumina.R
import com.example.projectilumina.Utils.NotificationUtils
import com.example.projectilumina.data.User
import com.example.projectilumina.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfileAdapter
    private lateinit var binding: ActivityProfileBinding
    private val perfilInfoList = mutableListOf<User>()
    private lateinit var notificationIcon: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)


        recyclerView = binding.recyclerViewPerfil
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = ProfileAdapter(perfilInfoList)
        recyclerView.adapter = adapter

        notificationIcon = findViewById(R.id.icon_notificacao)

        NotificationUtils.atualizarIconeNotificacao(notificationIcon)


        buscarDadosDoUsuario()
        setupNavigationButtons()

    }
    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Perfil"
        binding.appBarDefault.iconNotificacao.setOnClickListener {
            val intent = Intent(this, NotificacaoActivity::class.java)
            startActivity(intent)
        }

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
    private fun buscarDadosDoUsuario() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {

            val database = FirebaseDatabase.getInstance().getReference("users").child(userId)


            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    perfilInfoList.clear()

                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        perfilInfoList.add(user)
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Erro ao buscar dados", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        buscarDadosDoUsuario()
    }

}