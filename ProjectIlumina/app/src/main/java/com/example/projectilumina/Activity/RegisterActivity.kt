package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        auth = Firebase.auth

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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registrarUsuario() {
        val email: String = binding?.editTextEmail?.text.toString()
        val password: String = binding?.editTextPassword?.text.toString()
        val confirmpassword: String = binding?.editTextConfirmPassword?.text.toString()
        val nome: String = binding?.editTextNome?.text.toString()

        val cidadeSelecionada = binding?.spinnerCidade?.selectedItem?.toString() ?: ""
        val outraCidadeDigitada = binding?.editTextOutraCidade?.text?.toString() ?: ""

        val cidadeFinal = if (cidadeSelecionada == "Outra") outraCidadeDigitada else cidadeSelecionada

        if (email.isNotEmpty() && password.isNotEmpty() && confirmpassword.isNotEmpty()
            && nome.isNotEmpty() && cidadeFinal.isNotEmpty()
        ) {
            if (password == confirmpassword) {
                createUserWithEmailAndPassword(email, password, nome, cidadeFinal)
            } else {
                Toast.makeText(this@RegisterActivity, "Senhas incompatíveis.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@RegisterActivity, "Por favor preencha todos os campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        nome: String,
        cidade: String,
    ) {
        if (email.isEmpty() || password.isEmpty() || nome.isEmpty() || cidade.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "createUserWithEmailAndPassword: Success")
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
                                                Log.d(TAG, "Token FCM salvo com sucesso")

                                                val cidadeTopico = normalizarCidadeParaTopico(cidade)
                                                val topic = "cidade_$cidadeTopico"

                                                FirebaseMessaging.getInstance()
                                                    .subscribeToTopic(topic)
                                                    .addOnCompleteListener { subscribeTask ->

                                                        if (subscribeTask.isSuccessful) {
                                                            Log.d(TAG, "Inscrito no tópico: $topic")
                                                        } else {
                                                            Log.e(
                                                                TAG,
                                                                "Falha ao inscrever no tópico: ${subscribeTask.exception?.message}"
                                                            )
                                                        }

                                                        auth.currentUser?.sendEmailVerification()
                                                            ?.addOnSuccessListener {
                                                                Toast.makeText(
                                                                    this@RegisterActivity,
                                                                    "Cadastro criado! Verifique seu e-mail para ativar a conta.",
                                                                    Toast.LENGTH_LONG
                                                                ).show()


                                                                auth.signOut()

                                                                val intent = Intent(
                                                                    this@RegisterActivity,
                                                                    MainActivity::class.java
                                                                )
                                                                startActivity(intent)
                                                                finish()
                                                            }
                                                            ?.addOnFailureListener { e ->
                                                                Toast.makeText(
                                                                    this@RegisterActivity,
                                                                    "Cadastro criado, mas erro ao enviar verificação: ${e.message}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()

                                                                auth.signOut()
                                                                val intent = Intent(
                                                                    this@RegisterActivity,
                                                                    MainActivity::class.java
                                                                )
                                                                startActivity(intent)
                                                                finish()
                                                            }
                                                    }

                                            } else {
                                                Toast.makeText(
                                                    this@RegisterActivity,
                                                    "Erro ao salvar token FCM",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                Log.e(
                                                    TAG,
                                                    "Erro ao salvar token: ${tokenSaveTask.exception?.message}"
                                                )
                                            }
                                        }
                                } else {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "Erro ao obter token FCM",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.e(
                                        TAG,
                                        "Erro ao obter token FCM: ${tokenTask.exception?.message}"
                                    )
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Falha ao salvar dados: ${saveTask.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                val errorMessage = task.exception?.message ?: "Falha na autenticação"
                Log.w(TAG, "createUserWithEmailAndPassword: Failure", task.exception)
                Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun normalizarCidadeParaTopico(cidade: String): String {
        return cidade.trim()
            .lowercase()
            .replace("\\s+".toRegex(), "_")
    }

    companion object {
        private const val TAG = "EmailAndPassword"
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
