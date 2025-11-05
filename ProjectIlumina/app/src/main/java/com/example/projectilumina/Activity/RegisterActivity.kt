package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        binding?.buttonRegister?.setOnClickListener{
            val email: String = binding?.editTextEmail?.text.toString()
            val password: String = binding?.editTextPassword?.text.toString()
            val confirmpassword: String = binding?.editTextConfirmPassword?.text.toString()
            val nome: String = binding?.editTextNome?.text.toString()
            val cidade: String = binding?.editTextCidade?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmpassword.isNotEmpty() && nome.isNotEmpty() && cidade.isNotEmpty()) {
                if (password == confirmpassword) {
                    createUserWithEmailAndPassword(email, password, nome, cidade)
                } else {
                    Toast.makeText(this@RegisterActivity, "Senhas incompatíveis.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@RegisterActivity, "Por favor preencha todos os campos.", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.buttonLogin?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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
                                    database.child(userId).child("token").setValue(token).addOnCompleteListener { tokenSaveTask ->
                                        if (tokenSaveTask.isSuccessful) {
                                            Log.d(TAG, "Token FCM salvo com sucesso")
                                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this@RegisterActivity, "Erro ao salvar token FCM", Toast.LENGTH_SHORT).show()
                                            Log.e(TAG, "Erro ao salvar token: ${tokenSaveTask.exception?.message}")
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@RegisterActivity, "Erro ao obter token FCM", Toast.LENGTH_SHORT).show()
                                    Log.e(TAG, "Erro ao obter token FCM: ${tokenTask.exception?.message}")
                                }
                            }
                        } else {
                            Toast.makeText(this@RegisterActivity, "Falha ao salvar dados: ${saveTask.exception?.message}", Toast.LENGTH_SHORT).show()
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


    companion object {
        private const val TAG = "EmailAndPassword"
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}

