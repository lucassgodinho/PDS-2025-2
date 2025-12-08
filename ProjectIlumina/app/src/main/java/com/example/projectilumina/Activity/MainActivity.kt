package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectilumina.R
import com.example.projectilumina.data.User
import com.example.projectilumina.databinding.ActivityMainBinding
import com.example.projectilumina.Utils.LoadingDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var binding: ActivityMainBinding? = null
    private lateinit var edtEmail: EditText
    private lateinit var btnForgotPassword: Button
    private lateinit var loading: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        edtEmail = findViewById(R.id.editTextEmail)
        btnForgotPassword = findViewById(R.id.tvForgotPassword)

        loading = LoadingDialog(this)

        btnForgotPassword.setOnClickListener {
            enviarEmailDeReset()
        }

        binding?.buttonLogin?.setOnClickListener {

            val email = binding?.editTextEmail?.text.toString()
            val password = binding?.editTextPassword?.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor preencha os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loading.show()
            binding?.buttonLogin?.isEnabled = false

            signInWithEmailAndPassword(email, password)
        }

        binding?.buttonRegister?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding?.passwordToggle?.setOnClickListener {
            val isVisible = binding?.editTextPassword?.transformationMethod == null

            if (isVisible) {
                binding?.editTextPassword?.transformationMethod =
                    android.text.method.PasswordTransformationMethod.getInstance()
                binding?.passwordToggle?.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding?.editTextPassword?.transformationMethod = null
                binding?.passwordToggle?.setImageResource(R.drawable.ic_visibility)
            }

            binding?.editTextPassword?.setSelection(binding?.editTextPassword?.text!!.length)
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                loading.hide()
                binding?.buttonLogin?.isEnabled = true

                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        Toast.makeText(this, "Erro: usuário não autenticado", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    if (!currentUser.isEmailVerified) {
                        auth.signOut()
                        Toast.makeText(this, "Verifique seu e-mail antes de entrar.", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    val userId = currentUser.uid

                    FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .get()
                        .addOnSuccessListener { dataSnapshot ->
                            val user = dataSnapshot.getValue(User::class.java)
                            if (user != null) {
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                    if (tokenTask.isSuccessful) {
                                        val token = tokenTask.result
                                        FirebaseDatabase.getInstance().getReference("users")
                                            .child(userId)
                                            .child("token")
                                            .setValue(token)
                                    }
                                }
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Usuário não encontrado no banco de dados", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Erro ao buscar dados do usuário", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Usuário não encontrado ou senha incorreta", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun enviarEmailDeReset() {

        val email = edtEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Digite seu e-mail primeiro.", Toast.LENGTH_SHORT).show()
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
                    "Enviamos um link para redefinir sua senha.",
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
