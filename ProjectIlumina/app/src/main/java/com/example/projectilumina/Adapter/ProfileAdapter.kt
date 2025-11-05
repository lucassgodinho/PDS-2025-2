package com.example.projectilumina.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectilumina.R
import com.example.projectilumina.data.User

class ProfileAdapter(private val perfilList: List<User>) : RecyclerView.Adapter<ProfileAdapter.PerfilViewHolder>() {

    class PerfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.textNome)
        val email: TextView = itemView.findViewById(R.id.textEmail)
        val cidade: TextView = itemView.findViewById(R.id.textCidade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerfilViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return PerfilViewHolder(view)
    }

    override fun onBindViewHolder(holder: PerfilViewHolder, position: Int) {
        val perfil = perfilList[position]
        holder.nome.text = "Nome: ${perfil.nome}"
        holder.email.text = "Email: ${perfil.email}"
        holder.cidade.text = "Cidade: ${perfil.cidade}"


    }

    override fun getItemCount(): Int {
        return perfilList.size
    }
}