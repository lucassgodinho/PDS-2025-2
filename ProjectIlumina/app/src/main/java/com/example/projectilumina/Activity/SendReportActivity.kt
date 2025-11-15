package com.example.projectilumina.Activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.net.Uri
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
import com.example.projectilumina.databinding.ActivitySendReportBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

        obterLocalizacao()
        setupNavigationButtons()

        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    imageUri = result.data?.data
                    if (imageUri != null) {
                        Toast.makeText(this, "Imagem selecionada!", Toast.LENGTH_SHORT).show()
                        Log.d("IMAGE_URI", "URI da Imagem: $imageUri")
                    } else {
                        Log.e("IMAGE_URI", "Erro ao capturar a URI da imagem")
                        Toast.makeText(this, "Erro ao selecionar a imagem", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
    }

    private fun setupNavigationButtons() {
        binding.appBarDefault.textActivity.text = "Adicionar Denuncia"
        binding.appBarDefault.iconNotificacao.setOnClickListener {
            val intent = Intent(this, NotificacaoActivity::class.java)
            startActivity(intent)
        }

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
        binding.btnSelecionarImagem.setOnClickListener {
            selecionarImagem()
        }
        binding.btnConcluir.setOnClickListener {
            if (binding.btnConcluir.isEnabled) {
                binding.btnConcluir.isEnabled = false
                enviarDenuncia()
            }
        }
        binding.checkTorres.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkCapao.isChecked = false
            }
        }

        binding.checkCapao.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkTorres.isChecked = false
            }
        }

    }

    private fun obterLocalizacao() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            userLocation = location
            if (location != null) {
                Toast.makeText(
                    this,
                    "Localização obtida: ${location.latitude}, ${location.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun selecionarImagem() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun enviarDenuncia() {
        val problema = binding.edtProblema.text.toString().trim()
        val currentDate =
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val descricao = binding.edtDescricao.text.toString().trim()
        val rua = binding.edtRua.text.toString().trim()
        val bairro = binding.edtBairro.text.toString().trim()
        val cidade = binding.edtCidade.text.toString().trim()

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

        val uploadTask = storageRef.putFile(imageUri!!)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: Exception("Erro desconhecido ao enviar a imagem")
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            binding.btnConcluir.isEnabled = false
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
                    userId = userId
                )

                salvarDenuncia(denuncia)
            } else {
                Log.e("FirebaseStorage", "Erro ao obter URL da imagem: ${task.exception?.message}")
                Toast.makeText(this, "Erro ao enviar a imagem. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun salvarDenuncia(denuncia: Denuncia) {
        val reportsRef = FirebaseDatabase.getInstance().getReference("denuncias")

        val denunciaRef = if (denuncia.id.isEmpty()) {
            val newDenunciaRef = reportsRef.push()
            denuncia.id = newDenunciaRef.key ?: return
            newDenunciaRef
        } else {
            reportsRef.child(denuncia.id)
        }

        denunciaRef.setValue(denuncia).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                criarDenunciaEEnviarNotificacoes(
                    denuncia.id,
                    denuncia.latitude,
                    denuncia.longitude,
                    denuncia.dataHora,
                    denuncia.problema,
                    denuncia.descricao,
                    denuncia.userId
                )

                abrirEmailPrefeitura(denuncia, nomeUsuarioLogado)


                Toast.makeText(this, "Denúncia enviada/atualizada com sucesso!", Toast.LENGTH_SHORT)
                    .show()
                finish()
                binding.btnConcluir.isEnabled = false
            } else {
                binding.btnConcluir.isEnabled = true
                Log.e(
                    "FirebaseError",
                    "Erro ao salvar/atualizar denúncia: ${task.exception?.message}"
                )
            }
        }
    }
    private fun abrirEmailPrefeitura(denuncia: Denuncia, nomeUsuario: String) {
        val emailDestino: String

        val torresMarcado = binding.checkTorres.isChecked
        val capaoMarcado = binding.checkCapao.isChecked

        if (!torresMarcado && !capaoMarcado) {
            return
        }

        if (torresMarcado && capaoMarcado) {
            Toast.makeText(this, "Selecione apenas uma cidade para enviar o e-mail.", Toast.LENGTH_SHORT).show()
            return
        }

        emailDestino = if (torresMarcado) {

            "lucassgodinho@icloud.com"
        } else {
            "lucasgodinho@rede.ulbra.br"
        }

        val assunto = "Denúncia de Iluminação Pública"
        val corpoEmail = """
            Uma nova denúncia foi registrada pelo aplicativo ProjectIlumina.
            
            Nome do usuário: $nomeUsuario
            Bairro: ${denuncia.bairro}
            Rua: ${denuncia.rua}
            Problema: ${denuncia.problema}
            Descrição: ${denuncia.descricao}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailDestino))
            putExtra(Intent.EXTRA_SUBJECT, assunto)
            putExtra(Intent.EXTRA_TEXT, corpoEmail)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Nenhum aplicativo de e-mail encontrado.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun carregarNomeUsuario() {
        val uid = auth.currentUser?.uid ?: return

        usersRef.child(uid).get().addOnSuccessListener { snapshot ->
            val nome = snapshot.child("nome").getValue(String::class.java)

            if (!nome.isNullOrBlank()) {
                nomeUsuarioLogado = nome
                Log.d("USUARIO", "Nome do usuário carregado: $nomeUsuarioLogado")
            } else {
                Log.d("USUARIO", "Nome do usuário não encontrado, usando padrão.")
            }
        }.addOnFailureListener { e ->
            Log.e("USUARIO", "Erro ao carregar nome do usuário", e)
        }
    }



    fun criarDenunciaEEnviarNotificacoes(
        denunciaId: String,
        denunciaLatitude: Double,
        denunciaLongitude: Double,
        data: String,
        tipoManutencao: String,
        descricao: String,
        userIdDenunciante: String

    ) {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("users")

        usuariosRef.get().addOnSuccessListener { snapshot ->
            for (usuarioSnapshot in snapshot.children) {
                val userId = usuarioSnapshot.key


                if (userId == userIdDenunciante) {
                    continue
                }

                val userLatitude = usuarioSnapshot.child("latitude").getValue(Double::class.java)
                val userLongitude = usuarioSnapshot.child("longitude").getValue(Double::class.java)
                val userToken = usuarioSnapshot.child("token").getValue(String::class.java)

                if (userLatitude != null && userLongitude != null && userToken != null) {
                    val distancia = calcularDistancia(
                        denunciaLatitude,
                        denunciaLongitude,
                        userLatitude,
                        userLongitude
                    )

                    if (distancia <= 1.0) {
                        salvarNotificacaoParaUsuarioProximo(
                            userId!!,
                            denunciaId,
                            data,
                            tipoManutencao,
                            descricao
                        )
                    }
                }
            }
        }.addOnFailureListener {
            Log.e("Erro", "Erro ao acessar dados dos usuários", it)
        }
    }


    private fun salvarNotificacaoParaUsuarioProximo(
        userId: String,
        denunciaId: String,
        data: String,
        tipoManutencao: String,
        descricao: String,
        status: Boolean = false
    ) {
        val notificacao = mapOf(
            "denunciaId" to denunciaId,
            "data" to data,
            "tipoManutencao" to tipoManutencao,
            "descricao" to descricao,
            "status" to status
        )

        val notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes/$userId")
        notificacoesRef.push().setValue(notificacao)
    }

    private fun calcularDistancia(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
