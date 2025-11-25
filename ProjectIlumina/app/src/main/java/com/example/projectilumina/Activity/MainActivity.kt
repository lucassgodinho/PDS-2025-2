package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectilumina.R

import com.example.projectilumina.data.User
import com.example.projectilumina.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private var REQ_ONE_TAP = 2
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private var binding: ActivityMainBinding? = null
    private lateinit var edtEmail: EditText
    private lateinit var btnForgotPassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        auth = Firebase.auth

        edtEmail = findViewById(R.id.editTextEmail)
        btnForgotPassword = findViewById(R.id.tvForgotPassword)

        btnForgotPassword.setOnClickListener {
            enviarEmailDeReset()
        }

        binding?.buttonLogin?.setOnClickListener {
            val email: String = binding?.editTextEmail?.text.toString()
            val password: String = binding?.editTextPassword?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInWithEmailAndPassword(email, password)

            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Por favor preencha os campos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding?.buttonRegister?.setOnClickListener {
            val intent = Intent(this@MainActivity, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding?.passwordToggle?.setOnClickListener {
            val isPasswordVisible = binding?.editTextPassword?.transformationMethod == null

            if (isPasswordVisible) {

                binding?.editTextPassword?.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                binding?.passwordToggle?.setImageResource(R.drawable.ic_visibility_off)
            } else {

                binding?.editTextPassword?.transformationMethod = null
                binding?.passwordToggle?.setImageResource(R.drawable.ic_visibility)
            }


            binding?.editTextPassword?.setSelection(binding?.editTextPassword?.text!!.length)
        }


    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val currentUser = auth.currentUser
                if (currentUser != null) {

                    if (!currentUser.isEmailVerified) {
                        auth.signOut()
                        Toast.makeText(
                            this,
                            "Verifique seu e-mail antes de entrar.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    val userId = currentUser.uid

                    FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .get().addOnSuccessListener { dataSnapshot ->

                            val user = dataSnapshot.getValue(User::class.java)
                            if (user != null) {

                                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                    if (tokenTask.isSuccessful) {
                                        val userToken = tokenTask.result

                                        FirebaseDatabase.getInstance().getReference("users")
                                            .child(userId)
                                            .child("token")
                                            .setValue(userToken)
                                    } else {
                                        Toast.makeText(this, "Erro ao obter token FCM", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()

                            } else {
                                Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                            }

                        }.addOnFailureListener {
                            Toast.makeText(this, "Erro ao buscar dados do usuário", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    Toast.makeText(this, "Erro: usuário não autenticado", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Usuário não encontrado ou senha incorreta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarEmailDeReset() {
        val email = edtEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Digite seu e-mail primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "E-mail inválido", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Enviamos um link para redefinir sua senha no seu e-mail.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao enviar link: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


}

