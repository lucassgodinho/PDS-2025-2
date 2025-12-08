package com.example.projectilumina.Activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projectilumina.Adapter.InfoAdapter
import com.example.projectilumina.R
import com.example.projectilumina.Utils.NotificationManagerHelper
import com.example.projectilumina.Utils.NotificationUtils
import com.example.projectilumina.data.Denuncia
import com.example.projectilumina.databinding.ActivityHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeActivity : AppCompatActivity(), OnMapReadyCallback{
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityHomeBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var notificationIcon: ImageButton
    private val REQUEST_NOTIFICATION_PERMISSION = 1234
    private var userLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().getReference("denuncias")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        notificationIcon = findViewById(R.id.icon_notificacao)
        NotificationUtils.atualizarIconeNotificacao(notificationIcon)

        verificarPermissaoNotificacao()

        inscreverUsuarioNoTopicoDaCidade()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupNavigationButtons()
        updateIconColors(isMapaActivity = true)


    }

    private fun verificarPermissaoNotificacao() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                NotificationManagerHelper.verificarEEnviarNotificacao(this)
            }

        } else {
            NotificationManagerHelper.verificarEEnviarNotificacao(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                NotificationManagerHelper.verificarEEnviarNotificacao(this)
            } else {

                Toast.makeText(this, "Permissão para notificações negada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigationButtons() {
        binding.appBarHome.textActivity.text = "Mapa"
        binding.appBarHome.iconNotificacao.setOnClickListener {
            val intent = Intent(this, NotificacaoActivity::class.java)
            startActivity(intent)
        }
        binding.appBarHome.iconMenu.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        binding.endBar.iconDenuncia.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }
        binding.endBar.iconFeed.setOnClickListener{
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }
        val btnFiltrar = findViewById<Button>(R.id.btnFilter)
        btnFiltrar.setOnClickListener {
            abrirFiltro()
        }

    }
    private var filtroAtual: String? = null

    private fun abrirFiltro() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.item_filter)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(android.view.Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(true)

        val finalizados = dialog.findViewById<Button>(R.id.button_finalizados)
        val pendentes   = dialog.findViewById<Button>(R.id.button_pendentes)


        finalizados.setOnClickListener {
            if (filtroAtual == "finalizado") {
                carregarDenuncias()
                filtroAtual = null
                Toast.makeText(this, "Exibindo todas as denúncias", Toast.LENGTH_SHORT).show()
            } else {
                carregarDenunciasFiltradas("finalizado")
                filtroAtual = "finalizado"
                Toast.makeText(this, "Exibindo denúncias finalizadas", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        pendentes.setOnClickListener {
            if (filtroAtual == "pendente") {
                carregarDenuncias()
                filtroAtual = null
                Toast.makeText(this, "Exibindo todas as denúncias", Toast.LENGTH_SHORT).show()
            } else {
                carregarDenunciasFiltradas("pendente")
                filtroAtual = "pendente"
                Toast.makeText(this, "Exibindo denúncias pendentes", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }



    private fun carregarDenunciasFiltradas(statusFiltro: String) {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear()
                for (denunciaSnapshot in snapshot.children) {
                    val denuncia = denunciaSnapshot.getValue(Denuncia::class.java)
                    denuncia?.let {
                        if (it.status.trim().equals(statusFiltro, ignoreCase = true)) {
                            adicionarMarcadorNoMapa(it)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Erro ao filtrar denúncias: ${error.message}")
            }
        })
    }

    private fun updateIconColors(isMapaActivity: Boolean) {

        if (isMapaActivity) {
            binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.color_app_bar))
            binding.endBar.iconDenuncia.setColorFilter(ContextCompat.getColor(this, R.color.black))
        } else {
            binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.black))
            binding.endBar.iconDenuncia.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        mMap.isMyLocationEnabled = true
        getDeviceLocation()
    }

    private fun getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        updateUserLocationOnHomeScreen(it.latitude, it.longitude)
                    } ?: run {
                        Log.d("HomeActivity", "Localização não encontrada.")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun sendLocationToFirebase(latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
        databaseRef.updateChildren(locationData)
            .addOnSuccessListener {
                Log.d("HomeActivity", "Localização enviada para o Firebase com sucesso.")
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Erro ao enviar localização: ${e.message}")
            }
    }



    private fun updateUserLocationOnHomeScreen(latitude: Double, longitude: Double) {
        val userLat = LatLng(latitude, longitude)
        userLatLng = userLat
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLat, 15f))

        sendLocationToFirebase(latitude, longitude)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        carregarDenuncias()

        val infoAdapter = InfoAdapter(this)
        mMap.setInfoWindowAdapter(infoAdapter)

        mMap.setOnInfoWindowClickListener { marker ->
            val denuncia = marker.tag as? Denuncia ?: return@setOnInfoWindowClickListener

            if (denuncia.status.trim().equals("pendente", ignoreCase = true)) {
                verificarSePodeFinalizar(denuncia)
            } else {
                Toast.makeText(this, "Esta denúncia já está finalizada.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun verificarSePodeFinalizar(denuncia: Denuncia) {
        val usuario = userLatLng
        if (usuario == null) {
            Toast.makeText(this, "Não foi possível obter sua localização.", Toast.LENGTH_SHORT).show()
            return
        }
        val resultado = FloatArray(1)
        android.location.Location.distanceBetween(
            usuario.latitude,
            usuario.longitude,
            denuncia.latitude,
            denuncia.longitude,
            resultado
        )

        val distanciaMetros = resultado[0]

        val raioMaximo = 1000f

        if (distanciaMetros <= raioMaximo) {

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Finalizar denúncia")
                .setMessage("Você está perto desta denúncia. Deseja marcá-la como finalizada?")
                .setPositiveButton("Sim") { _, _ ->
                    finalizarDenuncia(denuncia)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            Toast.makeText(
                this,
                "Você precisa estar mais perto da denúncia para finalizá-la.",
                Toast.LENGTH_LONG
            ).show()
        }
    }




    private fun carregarDenuncias() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear()

                for (denunciaSnapshot in snapshot.children) {
                    val denuncia = denunciaSnapshot.getValue(Denuncia::class.java)
                    denuncia?.let {
                        adicionarMarcadorNoMapa(it)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Erro ao carregar denúncias: ${error.message}")
            }
        })
    }



    private fun adicionarMarcadorNoMapa(denuncia: Denuncia) {
        val localizacao = LatLng(denuncia.latitude, denuncia.longitude)

        val statusNormalizado = denuncia.status
            .trim()
            .lowercase()

        val iconResId = when (statusNormalizado) {
            "finalizado", "finalizada" -> R.drawable.icon_mapa_finalizadas
            "pendente", "pendentes"    -> R.drawable.icon_mapa_pendente
            else -> R.drawable.publicidade
        }


        val (width, height) = when (iconResId) {
            R.drawable.publicidade -> 100 to 100
            R.drawable.icon_mapa_pendente -> 100 to 100
            R.drawable.icon_mapa_finalizadas -> 100 to 100
            else -> 100 to 100
        }

        val iconDescriptor = bitmapDescriptorFromVector(iconResId, width, height)

        mMap.addMarker(
            MarkerOptions()
                .position(localizacao)
                .icon(iconDescriptor)
        )?.tag = denuncia
    }

    private fun bitmapDescriptorFromVector(drawableResId: Int, width: Int, height: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableResId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val bmpWidth = if (drawable.intrinsicWidth > 0) width else width
        val bmpHeight = if (drawable.intrinsicHeight > 0) height else height

        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }



    fun finalizarDenuncia(denuncia: Denuncia) {
        if (denuncia.id.isEmpty()) {
            Toast.makeText(this, "ID da denúncia é inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        val denunciaRef = FirebaseDatabase.getInstance()
            .getReference("denuncias")
            .child(denuncia.id)

        val updates = mapOf(
            "status" to "Finalizado"
        )

        denunciaRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Denúncia finalizada com sucesso.", Toast.LENGTH_SHORT).show()
                carregarDenuncias()
            }
            .addOnFailureListener { e ->
                Log.e("FinalizarDenuncia", "Erro ao finalizar denúncia: ${e.message}")
                Toast.makeText(this, "Erro ao finalizar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun inscreverUsuarioNoTopicoDaCidade() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        userRef.child("cidade").get().addOnSuccessListener { snapshot ->

            val cidade = snapshot.getValue(String::class.java)?.trim() ?: return@addOnSuccessListener
            val topico = normalizarCidadeParaTopico(cidade) ?: return@addOnSuccessListener

            FirebaseMessaging.getInstance()
                .subscribeToTopic(topico)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("TOPIC", "Inscrito no tópico: $topico")
                    } else {
                        Log.e("TOPIC", "Erro ao inscrever no tópico", task.exception)
                    }
                }
        }
    }

    private fun normalizarCidadeParaTopico(cidade: String): String? {
        val c = cidade.lowercase(Locale.getDefault()).trim()

        return when {
            c.contains("torres") -> "torres"
            c.contains("capao") || c.contains("capão") -> "capao"
            else -> null
        }
    }



}