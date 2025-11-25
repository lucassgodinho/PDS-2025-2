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
import com.example.projectilumina.databinding.ActivityReportBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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
            startActivity(Intent(this, SendReportActivity::class.java))
        }

        recyclerView = findViewById(R.id.recyclerViewDenuncias)
        recyclerView.layoutManager = LinearLayoutManager(this)

        denunciaList = ArrayList()


        denunciaAdapter = DenunciaAdapter(denunciaList) { denuncia ->
            val intent = Intent(this, UpdateReport::class.java)
            intent.putExtra("DENUNCIA_ID", denuncia.id)
            startActivity(intent)
        }

        recyclerView.adapter = denunciaAdapter

        database = FirebaseDatabase.getInstance().getReference("denuncias")

        carregarDenuncias()

        notificationIcon = findViewById(R.id.icon_notificacao)
        NotificationUtils.atualizarIconeNotificacao(notificationIcon)

        updateIconColors(true)
        setupNavigationButtons()
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
                        val id = denunciaSnapshot.key

                        if (denuncia != null && id != null) {
                            denuncia.id = id
                            denunciaList.add(denuncia)
                        }
                    }

                    denunciaAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Minhas denúncias"

        binding.appBarDefault.iconNotificacao.setOnClickListener {
            startActivity(Intent(this, NotificacaoActivity::class.java))
        }

        binding.appBarDefault.iconMenu.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.endBar.iconMapa.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        binding.endBar.iconFeed.setOnClickListener {
            startActivity(Intent(this, FeedActivity::class.java))
        }
    }

    private fun updateIconColors(isReportActivity: Boolean) {
        binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.black))
        binding.endBar.iconDenuncia.setColorFilter(
            ContextCompat.getColor(
                this,
                if (isReportActivity) R.color.color_app_bar else R.color.black
            )
        )
    }
}
