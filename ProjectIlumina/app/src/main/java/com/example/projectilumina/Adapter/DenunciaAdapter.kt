package com.example.projectilumina.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectilumina.R
import com.example.projectilumina.data.Denuncia

class DenunciaAdapter(
    private val denunciaList: List<Denuncia>,
    private val onItemClick: (Denuncia) -> Unit) :
    RecyclerView.Adapter<DenunciaAdapter.DenunciaViewHolder>() {

    inner class DenunciaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRua = itemView.findViewById<TextView>(R.id.tvRua)
        private val tvCidade = itemView.findViewById<TextView>(R.id.tvCidade)
        private val tvBairro = itemView.findViewById<TextView>(R.id.tvBairro)
        private val tvProblema = itemView.findViewById<TextView>(R.id.tvProblema)
        private val tvDataHora = itemView.findViewById<TextView>(R.id.tvDataHora)
        private val tvDescricao = itemView.findViewById<TextView>(R.id.tvDescricao)
        private val tvImagem = itemView.findViewById<ImageView>(R.id.tvImagem)
        private var tvStatusColor = itemView.findViewById<View>(R.id.tvStatusColor)
        private var tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)



        fun bind(denuncia: Denuncia) {
            tvCidade.text = "Cidade: ${denuncia.cidade}"
            tvBairro.text = "Bairro: ${denuncia.bairro}"
            tvRua.text = "Rua: ${denuncia.rua}"
            tvProblema.text = "Problema: ${denuncia.problema}"
            tvDataHora.text = "Data e Hora: ${denuncia.dataHora}"
            tvDescricao.text = "Descrição: ${denuncia.descricao}"
            tvStatus.text = "${denuncia.status}"
            val statusColor = when (denuncia.status) {
                "Em Andamento" -> ContextCompat.getColor(tvStatus.context, R.color.blue)
                "Finalizado" -> ContextCompat.getColor(tvStatus.context, R.color.greenStatus)
                else -> ContextCompat.getColor(tvStatus.context, R.color.red)
            }
            tvStatusColor.setBackgroundColor(statusColor)


            if (denuncia.imagemUrl != null) {
                Glide.with(itemView.context)
                    .load(denuncia.imagemUrl)
                    .placeholder(R.drawable.carregando_img)
                    .error(R.drawable.carregando_img)
                    .into(tvImagem)
            } else {
                tvImagem.setImageResource(R.drawable.icon_logo)
            }
            itemView.setOnClickListener {
                onItemClick(denuncia)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DenunciaViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_denuncia, parent, false)
        return DenunciaViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DenunciaViewHolder, position: Int) {
        holder.bind(denunciaList[position])
    }

    override fun getItemCount() = denunciaList.size
}