package com.example.projectilumina.Activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.projectilumina.R
import com.example.projectilumina.Utils.NotificationUtils
import com.example.projectilumina.data.Denuncia
import com.example.projectilumina.data.FeedItem
import com.example.projectilumina.databinding.ActivitySendReportBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Date
import java.util.Locale

class SendReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendReportBinding
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var userLocation: Location? = null
    private var imageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private lateinit var auth: FirebaseAuth
    private lateinit var notificationIcon: ImageButton
    private lateinit var usersRef: DatabaseReference

    private var nomeUsuarioLogado: String = "Usuário do aplicativo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySendReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().getReference("denuncias")
        storage = FirebaseStorage.getInstance().reference

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        auth = FirebaseAuth.getInstance()
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        carregarNomeUsuario()

        notificationIcon = findViewById(R.id.icon_notificacao)
        NotificationUtils.atualizarIconeNotificacao(notificationIcon)

        pedirPermissaoNotificacaoAndroid13()
        obterLocalizacao()
        setupNavigationButtons()

        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    imageUri = result.data?.data
                    if (imageUri != null) {
                        Toast.makeText(this, "Imagem selecionada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Erro ao selecionar a imagem", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun pedirPermissaoNotificacaoAndroid13() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001
                )
            }
        }
    }

    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Adicionar Denuncia"

        binding.appBarDefault.iconNotificacao.setOnClickListener {
            startActivity(Intent(this, NotificacaoActivity::class.java))
        }

        binding.appBarDefault.iconReturn.setOnClickListener { finish() }

        binding.endBar.iconDenuncia.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.endBar.iconMapa.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        binding.endBar.iconFeed.setOnClickListener {
            startActivity(Intent(this, FeedActivity::class.java))
        }

        binding.btnSelecionarImagem.setOnClickListener { selecionarImagem() }

        binding.btnConcluir.setOnClickListener {
            if (binding.btnConcluir.isEnabled) {
                binding.btnConcluir.isEnabled = false
                enviarDenuncia()
            }
        }

        binding.checkTorres.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.checkCapao.isChecked = false
        }

        binding.checkCapao.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.checkTorres.isChecked = false
        }
    }

    private fun obterLocalizacao() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            userLocation = location
        }
    }

    private fun selecionarImagem() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun enviarDenuncia() {
        val problema = binding.edtProblema.text.toString().trim()
        val descricao = binding.edtDescricao.text.toString().trim()
        val rua = binding.edtRua.text.toString().trim()
        val bairro = binding.edtBairro.text.toString().trim()
        val cidade = binding.edtCidade.text.toString().trim()

        val currentDate =
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val prefeituraDestino = when {
            binding.checkTorres.isChecked -> "Torres"
            binding.checkCapao.isChecked -> "Capão da Canoa"
            else -> null
        }

        if (problema.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            binding.btnConcluir.isEnabled = true
            return
        }

        val latitude = userLocation?.latitude ?: 0.0
        val longitude = userLocation?.longitude ?: 0.0
        val userId = auth.currentUser?.uid ?: return

        if (imageUri == null) {
            Toast.makeText(this, "Selecione uma imagem para a denúncia.", Toast.LENGTH_SHORT).show()
            binding.btnConcluir.isEnabled = true
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
            .child("imagens_denuncias/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(imageUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val imageUrl = task.result.toString()

                    val denuncia = Denuncia(
                        id = "",
                        cidade = cidade,
                        rua = rua,
                        bairro = bairro,
                        problema = problema,
                        dataHora = currentDate,
                        descricao = descricao,
                        latitude = latitude,
                        longitude = longitude,
                        imagemUrl = imageUrl,
                        userId = userId,
                        prefeituraDestino = prefeituraDestino
                    )

                    salvarDenuncia(denuncia)
                } else {
                    Toast.makeText(this, "Erro ao enviar a imagem.", Toast.LENGTH_SHORT).show()
                    binding.btnConcluir.isEnabled = true
                }
            }
    }

    private fun salvarDenuncia(denuncia: Denuncia) {
        val reportsRef = FirebaseDatabase.getInstance().getReference("denuncias")

        val denunciaRef = reportsRef.push()
        denuncia.id = denunciaRef.key ?: return

        denunciaRef.setValue(denuncia).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                abrirEmailPrefeitura(denuncia, nomeUsuarioLogado)

                Toast.makeText(this, "Denúncia enviada com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                binding.btnConcluir.isEnabled = true
                Toast.makeText(this, "Erro ao salvar denúncia.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirEmailPrefeitura(denuncia: Denuncia, nomeUsuario: String) {
        val torresMarcado = binding.checkTorres.isChecked
        val capaoMarcado = binding.checkCapao.isChecked
        if (!torresMarcado && !capaoMarcado) return

        if (torresMarcado && capaoMarcado) {
            Toast.makeText(this, "Selecione apenas uma cidade.", Toast.LENGTH_SHORT).show()
            return
        }

        val emailDestino = if (torresMarcado) {
            "lucassgodinho@icloud.com"
        } else {
            "lucasgodinho@rede.ulbra.br"
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailDestino))
            putExtra(Intent.EXTRA_SUBJECT, "Denúncia de Iluminação Pública")
            putExtra(
                Intent.EXTRA_TEXT,
                """
                Nova denúncia registrada no ProjectIlumina.
                
                Nome: $nomeUsuario
                Bairro: ${denuncia.bairro}
                Rua: ${denuncia.rua}
                Problema: ${denuncia.problema}
                Descrição: ${denuncia.descricao}
                """.trimIndent()
            )
        }

        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    }

    private fun carregarNomeUsuario() {
        val uid = auth.currentUser?.uid ?: return
        usersRef.child(uid).get().addOnSuccessListener { snapshot ->
            val nome = snapshot.child("nome").getValue(String::class.java)
            if (!nome.isNullOrBlank()) nomeUsuarioLogado = nome
        }
    }
}

