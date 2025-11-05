package com.example.projectilumina.Activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().getReference("denuncias")


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        notificationIcon = findViewById(R.id.icon_notificacao)

        NotificationUtils.atualizarIconeNotificacao(notificationIcon)

        verificarPermissaoNotificacao()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupNavigationButtons()
        updateIconColors(isMapaActivity = true)


    }
    private fun verificarPermissaoNotificacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION)

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
    }
    private fun updateIconColors(isMapaActivity: Boolean) {

        if (isMapaActivity) {
            binding.endBar.iconMapa.setColorFilter(ContextCompat.getColor(this, R.color.white))
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
        val userLatLng = LatLng(latitude, longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

        sendLocationToFirebase(latitude, longitude)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        carregarDenuncias()

        val infoAdapter = InfoAdapter(this)
        mMap.setInfoWindowAdapter(infoAdapter)

        mMap.setOnInfoWindowClickListener { marker ->
            val denuncia = marker.tag as? Denuncia
            denuncia?.let {
                infoAdapter.showFinalizarDialog(it)
            }
        }
    }

    private fun carregarDenuncias() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear()

                for (denunciaSnapshot in snapshot.children) {
                    val denuncia = denunciaSnapshot.getValue(Denuncia::class.java)
                    denuncia?.let {
                        if (!it.deletada) {
                            adicionarMarcadorNoMapa(it)
                        }
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

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.publicidade)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)

        mMap.addMarker(MarkerOptions()
            .position(localizacao)
            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))
            ?.tag = denuncia
    }

    fun finalizarDenuncia(denuncia: Denuncia) {
        if (denuncia.id.isEmpty()) {
            Toast.makeText(this, "ID da denúncia é inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        val denunciaRef = FirebaseDatabase.getInstance().getReference("denuncias").child(denuncia.id)
        val updates = mapOf(
            "status" to "Finalizado",
            "deletada" to true
        )


        denunciaRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Denúncia finalizada com sucesso.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FinalizarDenuncia", "Erro ao finalizar denúncia: ${e.message}")
                Toast.makeText(this, "Erro ao finalizar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}