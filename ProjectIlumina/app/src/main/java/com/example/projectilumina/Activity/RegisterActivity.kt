package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectilumina.Utils.LoadingDialog
import com.example.projectilumina.data.User
import com.example.projectilumina.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var binding: ActivityRegisterBinding? = null
    private lateinit var loading: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        auth = Firebase.auth

        loading = LoadingDialog(this)

        val adapter = ArrayAdapter.createFromResource(
            this,
            com.example.projectilumina.R.array.lista_cidades,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinnerCidade?.adapter = adapter

        binding?.spinnerCidade?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val cidadeSelecionada = parent?.getItemAtPosition(position).toString()
                if (cidadeSelecionada == "Outra") {
                    binding?.editTextOutraCidade?.visibility = View.VISIBLE
                } else {
                    binding?.editTextOutraCidade?.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding?.buttonRegister?.setOnClickListener {
            registrarUsuario()
        }

        binding?.buttonLogin?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun registrarUsuario() {
        val email = binding?.editTextEmail?.text.toString()
        val password = binding?.editTextPassword?.text.toString()
        val confirmpassword = binding?.editTextConfirmPassword?.text.toString()
        val nome = binding?.editTextNome?.text.toString()

        val cidadeSelecionada = binding?.spinnerCidade?.selectedItem?.toString() ?: ""
        val outraCidadeDigitada = binding?.editTextOutraCidade?.text?.toString() ?: ""

        val cidadeFinal = if (cidadeSelecionada == "Outra") outraCidadeDigitada else cidadeSelecionada

        if (email.isNotEmpty() && password.isNotEmpty() &&
            confirmpassword.isNotEmpty() && nome.isNotEmpty() && cidadeFinal.isNotEmpty()
        ) {
            if (password == confirmpassword) {
                createUserWithEmailAndPassword(email, password, nome, cidadeFinal)
            } else {
                Toast.makeText(this, "Senhas incompatíveis.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor preencha todos os campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        nome: String,
        cidade: String,
    ) {

        loading.show()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val user = auth.currentUser
                user?.let {
                    val userId = it.uid
                    val database = FirebaseDatabase.getInstance().getReference("users")

                    val latitude = 0.0
                    val longitude = 0.0

                    val userObj = User(userId, nome, email, cidade, latitude, longitude)

                    database.child(userId).setValue(userObj).addOnCompleteListener { saveTask ->
                        if (saveTask.isSuccessful) {

                            FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result

                                    database.child(userId).child("token").setValue(token)
                                        .addOnCompleteListener { tokenSaveTask ->

                                            if (tokenSaveTask.isSuccessful) {

                                                val cidadeTopico =
                                                    normalizarCidadeParaTopico(cidade)

                                                val topic = "cidade_$cidadeTopico"

                                                FirebaseMessaging.getInstance()
                                                    .subscribeToTopic(topic)
                                                    .addOnCompleteListener {

                                                        auth.currentUser?.sendEmailVerification()
                                                            ?.addOnSuccessListener {
                                                                loading.hide()

                                                                Toast.makeText(
                                                                    this,
                                                                    "Cadastro criado! Verifique seu e-mail.",
                                                                    Toast.LENGTH_LONG
                                                                ).show()

                                                                auth.signOut()
                                                                startActivity(
                                                                    Intent(
                                                                        this,
                                                                        MainActivity::class.java
                                                                    )
                                                                )
                                                                finish()
                                                            }
                                                            ?.addOnFailureListener { e ->
                                                                loading.hide()

                                                                Toast.makeText(
                                                                    this,
                                                                    "Cadastro criado, mas erro ao enviar verificação: ${e.message}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()

                                                                auth.signOut()
                                                                startActivity(Intent(this, MainActivity::class.java))
                                                                finish()
                                                            }
                                                    }

                                            } else {
                                                loading.hide()
                                                Toast.makeText(this, "Erro ao salvar token FCM", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    loading.hide()
                                    Toast.makeText(this, "Erro ao obter token FCM", Toast.LENGTH_SHORT).show()
                                }
                            }

                        } else {
                            loading.hide()
                            Toast.makeText(this, "Erro ao salvar dados do usuário.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } else {
                loading.hide()
                val errorMessage = task.exception?.message ?: "Falha ao criar conta"
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun normalizarCidadeParaTopico(cidade: String): String {
        return cidade.trim().lowercase().replace("\\s+".toRegex(), "_")
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
