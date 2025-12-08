package com.example.projectilumina.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectilumina.R
import com.example.projectilumina.data.User

class ProfileAdapter(
    private val perfilList: List<User>,
    private val onEditClick: () -> Unit,
    private val onDeleteClick: () -> Unit,
    private val onLogoutClick: () -> Unit
) : RecyclerView.Adapter<ProfileAdapter.PerfilViewHolder>() {

    class PerfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.textNome)
        val email: TextView = itemView.findViewById(R.id.textEmail)
        val cidade: TextView = itemView.findViewById(R.id.textCidade)
        val foto: ImageView = itemView.findViewById(R.id.imageProfile)
        val btnEdit: ImageButton = itemView.findViewById(R.id.button_edit_profile)
        val btnExcluir: ImageButton = itemView.findViewById(R.id.button_excluir_conta)
        val btnLogout: TextView = itemView.findViewById(R.id.sair_conta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerfilViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return PerfilViewHolder(view)
    }

    override fun onBindViewHolder(holder: PerfilViewHolder, position: Int) {
        val perfil = perfilList[position]

        holder.nome.text = "Nome: ${perfil.nome}"
        holder.email.text = "Email: ${perfil.email}"
        holder.cidade.text = "Cidade: ${perfil.cidade}"

        Glide.with(holder.itemView.context)
            .load(perfil.imgperfil)
            .placeholder(R.drawable.foto_perfil)
            .error(R.drawable.foto_perfil)
            .into(holder.foto)


        holder.btnEdit.setOnClickListener { onEditClick() }
        holder.btnExcluir.setOnClickListener { onDeleteClick() }
        holder.btnLogout.setOnClickListener { onLogoutClick() }
    }

    override fun getItemCount(): Int = perfilList.size
}
