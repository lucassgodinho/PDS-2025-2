package com.example.projectilumina.Activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectilumina.Adapter.ProfileAdapter
import com.example.projectilumina.R
import com.example.projectilumina.Utils.NotificationUtils
import com.example.projectilumina.data.User
import com.example.projectilumina.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val perfilList = ArrayList<User>()
    private lateinit var adapter: ProfileAdapter
    private lateinit var notificationIcon: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")
        notificationIcon = findViewById(R.id.icon_notificacao)
        NotificationUtils.atualizarIconeNotificacao(notificationIcon)

        setupRecycler()
        setupNavigationButtons()

        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun setupRecycler() {
        adapter = ProfileAdapter(
            perfilList,
            onEditClick = {
                startActivity(Intent(this, UpdateProfile::class.java))
            },
            onDeleteClick = { showDeleteConfirmationDialog() },
            onLogoutClick = { logout() }
        )

        binding.recyclerViewPerfil.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPerfil.adapter = adapter
    }
    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Perfil"

        binding.appBarDefault.iconReturn.setOnClickListener {
            finish()
        }

        binding.endBar.iconDenuncia.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
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

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                perfilList.clear()

                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    val usuarioFinal = user.copy(id = uid)
                    perfilList.add(usuarioFinal)
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Excluir conta")
        builder.setMessage("Tem certeza que deseja excluir sua conta? Essa ação é irreversível.")

        builder.setPositiveButton("Sim") { _, _ ->
            executarExclusao()
        }

        builder.setNegativeButton("Não") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun executarExclusao() {
        val uid = auth.currentUser?.uid ?: return

        database.child(uid).removeValue()


        auth.currentUser?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(this, "Conta excluída!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            ?.addOnFailureListener {
                Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_LONG).show()
            }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Você saiu da conta!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
