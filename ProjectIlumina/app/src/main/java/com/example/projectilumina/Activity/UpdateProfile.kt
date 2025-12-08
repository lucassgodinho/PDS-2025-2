package com.example.projectilumina.Activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.projectilumina.R
import com.example.projectilumina.Utils.LoadingDialog
import com.example.projectilumina.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class UpdateProfile : AppCompatActivity() {

    private lateinit var edtNome: EditText
    private lateinit var edtCidade: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnVoltar: ImageButton
    private lateinit var btnTrocarFoto: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var imgPerfilUrlAtual: String? = null
    private var novaImagemUri: Uri? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var loading: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")
        loading = LoadingDialog(this)

        btnVoltar = findViewById(R.id.buttonLogin)
        edtNome = findViewById(R.id.editTextNome)
        edtCidade = findViewById(R.id.editTextCidade)
        edtEmail = findViewById(R.id.editTextEmail)
        btnSalvar = findViewById(R.id.buttonRegister)
        btnTrocarFoto = findViewById(R.id.button_update_photo)

        configurarSelecionadorDeImagem()
        carregarDadosUsuario()

        btnVoltar.setOnClickListener { finish() }

        btnTrocarFoto.setOnClickListener {
            abrirGaleria()
        }

        btnSalvar.setOnClickListener {
            salvarAtualizacoes()
        }
    }

    private fun configurarSelecionadorDeImagem() {
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    novaImagemUri = result.data!!.data
                    Toast.makeText(this, "Nova foto selecionada!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun carregarDadosUsuario() {
        val usuarioAtual = auth.currentUser
        val uid = usuarioAtual?.uid

        if (uid == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }



        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val user = snapshot.getValue(User::class.java)

                if (user != null) {
                    edtNome.setText(user.nome ?: "")
                    edtCidade.setText(user.cidade ?: "")
                    edtEmail.setText(user.email ?: "")
                    imgPerfilUrlAtual = user.imgperfil
                } else {
                    Toast.makeText(
                        this@UpdateProfile,
                        "Dados do usuário não encontrados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(
                    this@UpdateProfile,
                    "Erro ao carregar dados",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun salvarAtualizacoes() {
        loading.show()
        val usuarioAtual = auth.currentUser
        val uid = usuarioAtual?.uid

        if (uid == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }

        val novoNome = edtNome.text.toString().trim()
        val novaCidade = edtCidade.text.toString().trim()
        val novoEmail = edtEmail.text.toString().trim()

        if (novoNome.isEmpty() || novaCidade.isEmpty() || novoEmail.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }



        if (novaImagemUri != null) {
            enviarNovaFotoEAtualizar(uid, novoNome, novaCidade, novoEmail)
        } else {
            atualizarDadosUsuario(uid, novoNome, novaCidade, novoEmail, imgPerfilUrlAtual)
        }
    }

    private fun enviarNovaFotoEAtualizar(
        uid: String,
        novoNome: String,
        novaCidade: String,
        novoEmail: String
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("imagens_perfil/$uid-${System.currentTimeMillis()}.jpg")

        storageRef.putFile(novaImagemUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Erro ao enviar imagem")
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                val novaUrl = uri.toString()
                imgPerfilUrlAtual = novaUrl
                atualizarDadosUsuario(uid, novoNome, novaCidade, novoEmail, novaUrl)
            }
            .addOnFailureListener {
                loading.hide()
                Toast.makeText(
                    this,
                    "Erro ao enviar nova foto de perfil",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun atualizarDadosUsuario(
        uid: String,
        novoNome: String,
        novaCidade: String,
        novoEmail: String,
        urlFoto: String?
    ) {
        val updates = mutableMapOf<String, Any>(
            "nome" to novoNome,
            "cidade" to novaCidade,
            "email" to novoEmail
        )

        if (!urlFoto.isNullOrEmpty()) {
            updates["imgperfil"] = urlFoto
        }

        database.child(uid).updateChildren(updates)
            .addOnSuccessListener {
                loading.hide()
                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                loading.hide()
                Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
            }
    }
}
