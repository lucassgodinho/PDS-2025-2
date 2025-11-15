package com.example.projectilumina.Adapter

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.projectilumina.Activity.HomeActivity
import com.example.projectilumina.R
import com.example.projectilumina.data.Denuncia
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class InfoAdapter(private val activity: HomeActivity) : GoogleMap.InfoWindowAdapter {

    private val window: View = LayoutInflater.from(activity).inflate(R.layout.item_denuncia_mapa, null).apply {
        alpha = 0.9f
    }

    override fun getInfoWindow(marker: Marker): View? {
        val dataTextView = window.findViewById<TextView>(R.id.data)
        val problemaTextView = window.findViewById<TextView>(R.id.tvProblema)
        val descricaoTextView = window.findViewById<TextView>(R.id.tvDescricao)

        val denuncia = marker.tag as? Denuncia
        denuncia?.let { denunciaInfo ->
            dataTextView.text = denunciaInfo.dataHora
            problemaTextView.text = denunciaInfo.problema
            descricaoTextView.text = denunciaInfo.descricao
        }

        return window
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    fun showFinalizarDialog(denuncia: Denuncia) {
        if (denuncia.id.isNullOrEmpty()) {
            return
        }

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Finalizar Denúncia")
            .setMessage("Você tem certeza que deseja finalizar esta denúncia?")
            .setPositiveButton("Sim") { dialog, _ ->
                activity.finalizarDenuncia(denuncia)
                dialog.dismiss()
            }
            .setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}