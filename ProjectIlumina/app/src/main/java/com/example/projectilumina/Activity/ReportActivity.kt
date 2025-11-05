package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectilumina.Adapter.DenunciaAdapter
import com.example.projectilumina.R
import com.example.projectilumina.Utils.NotificationUtils
import com.example.projectilumina.data.Denuncia
import com.example.projectilumina.databinding.ActivityProfileBinding
import com.example.projectilumina.databinding.ActivityReportBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ReportActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var denunciaAdapter: DenunciaAdapter
    private lateinit var denunciaList: ArrayList<Denuncia>
    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivityReportBinding
    private lateinit var notificationIcon: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAdicionarDenuncia.setOnClickListener {
            val intent = Intent(this, SendReportActivity::class.java)
            startActivity(intent)
        }



        recyclerView = findViewById(R.id.recyclerViewDenuncias)
        recyclerView.layoutManager = LinearLayoutManager(this)
        denunciaList = ArrayList()
        denunciaAdapter = DenunciaAdapter(denunciaList)

        recyclerView.adapter = denunciaAdapter


        database = FirebaseDatabase.getInstance().getReference("denuncias")
        notificationIcon = findViewById(R.id.icon_notificacao)

        NotificationUtils.atualizarIconeNotificacao(notificationIcon)

        setupNavigationButtons()
        carregarDenuncias()
        updateIconColors(isMapaActivity = true)
        NotificationUtils.atualizarIconeNotificacao(binding.appBarDefault.iconNotificacao)
    }

    override fun onStart() {
        super.onStart()
        NotificationUtils.atualizarIconeNotificacao(binding.appBarDefault.iconNotificacao)
    }
    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Minhas denuncias"
        binding.appBarDefault.iconNotificacao.setOnClickListener {
            val intent = Intent(this, NotificacaoActivity::class.java)
            startActivity(intent)
        }

        binding.appBarDefault.iconMenu.setOnClickListener {
            val intent = Intent(this, ActivityProfileBinding::class.java)
            startActivity(intent)
        }
        binding.endBar.iconMapa.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        binding.endBar.iconFeed.setOnClickListener{
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }
    }
    private fun updateIconColors(isMapaActivity: Boolean) {
        if (isMapaActivity) {

            binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.black))
            binding.endBar.iconDenuncia.setColorFilter(ContextCompat.getColor(this, R.color.white))
        } else {

            binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.black))
            binding.endBar.iconDenuncia.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }
    }
    private fun carregarDenuncias() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    denunciaList.clear()
                    for (denunciaSnapshot in snapshot.children) {
                        val denuncia = denunciaSnapshot.getValue(Denuncia::class.java)
                        denuncia?.let { denunciaList.add(it) }
                    }
                    denunciaAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}


