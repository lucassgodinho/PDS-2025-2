package com.example.projectilumina.Activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.projectilumina.R
import com.example.projectilumina.data.Denuncia
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class UpdateReport : AppCompatActivity() {

    private lateinit var edtRua: EditText
    private lateinit var edtCidade: EditText
    private lateinit var edtBairro: EditText
    private lateinit var edtProblema: EditText
    private lateinit var edtDescricao: EditText
    private lateinit var checkTorres: CheckBox
    private lateinit var checkCapao: CheckBox
    private lateinit var btnAtualizar: Button
    private lateinit var buttonreturn: ImageButton
    private lateinit var btnSelecionarImagem: Button

    private lateinit var database: DatabaseReference
    private lateinit var rootRef: DatabaseReference
    private var denunciaId: String? = null
    private var imagemUrlAtual: String? = null


    private var novaImagemUri: Uri? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_report)

        database = FirebaseDatabase.getInstance().getReference("denuncias")
        rootRef = FirebaseDatabase.getInstance().reference

        edtRua = findViewById(R.id.edt_rua)
        edtCidade = findViewById(R.id.edt_cidade)
        edtBairro = findViewById(R.id.edt_bairro)
        edtProblema = findViewById(R.id.edt_problema)
        edtDescricao = findViewById(R.id.edt_descricao)
        checkTorres = findViewById(R.id.check_torres)
        checkCapao = findViewById(R.id.check_capao)
        btnAtualizar = findViewById(R.id.btn_concluir)
        buttonreturn = findViewById(R.id.buttonreturn)
        btnSelecionarImagem = findViewById(R.id.btnSelecionarImagem)

        denunciaId = intent.getStringExtra("DENUNCIA_ID")

        if (denunciaId == null) {
            Toast.makeText(this, "Erro ao identificar denúncia!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarSelecionadorDeImagem()
        carregarDados()

        btnSelecionarImagem.setOnClickListener { abrirGaleria() }
        btnAtualizar.setOnClickListener { atualizar() }
        buttonreturn.setOnClickListener { finish() }

        checkTorres.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkCapao.isChecked = false
        }

        checkCapao.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkTorres.isChecked = false
        }
    }


    private fun configurarSelecionadorDeImagem() {
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    novaImagemUri = result.data!!.data
                    Toast.makeText(this, "Nova imagem selecionada!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun carregarDados() {
        database.child(denunciaId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val denuncia = snapshot.getValue(Denuncia::class.java)

                    if (denuncia != null) {
                        edtRua.setText(denuncia.rua)
                        edtCidade.setText(denuncia.cidade)
                        edtBairro.setText(denuncia.bairro)
                        edtProblema.setText(denuncia.problema)
                        edtDescricao.setText(denuncia.descricao)

                        imagemUrlAtual = denuncia.imagemUrl

                        when (denuncia.prefeituraDestino) {
                            "Torres" -> checkTorres.isChecked = true
                            "Capão da Canoa" -> checkCapao.isChecked = true
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun atualizar() {
        val rua = edtRua.text.toString().trim()
        val cidade = edtCidade.text.toString().trim()
        val bairro = edtBairro.text.toString().trim()
        val problema = edtProblema.text.toString().trim()
        val descricao = edtDescricao.text.toString().trim()

        if (rua.isEmpty() || cidade.isEmpty() || bairro.isEmpty() || problema.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        val prefeituraDestino =
            if (checkTorres.isChecked) "Torres"
            else if (checkCapao.isChecked) "Capão da Canoa"
            else ""

        if (novaImagemUri != null) {
            atualizarComNovaImagem(rua, cidade, bairro, problema, descricao, prefeituraDestino)
        } else {
            atualizarSemImagem(rua, cidade, bairro, problema, descricao, prefeituraDestino)
        }
    }

    private fun atualizarSemImagem(
        rua: String,
        cidade: String,
        bairro: String,
        problema: String,
        descricao: String,
        prefeituraDestino: String
    ) {
        val id = denunciaId!!

        val updates = mapOf<String, Any?>(
            "/denuncias/$id/rua" to rua,
            "/denuncias/$id/cidade" to cidade,
            "/denuncias/$id/bairro" to bairro,
            "/denuncias/$id/problema" to problema,
            "/denuncias/$id/descricao" to descricao,
            "/denuncias/$id/prefeituraDestino" to prefeituraDestino,
            "/denuncias/$id/imagemUrl" to imagemUrlAtual,

            "/feed/$id/cidade" to cidade,
            "/feed/$id/problema" to problema,
            "/feed/$id/descricao" to descricao,
            "/feed/$id/imagemUrl" to imagemUrlAtual
        )

        rootRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Denúncia e feed atualizados!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }



    private fun atualizarComNovaImagem(
        rua: String,
        cidade: String,
        bairro: String,
        problema: String,
        descricao: String,
        prefeituraDestino: String
    ) {
        val id = denunciaId!!
        val storageRef = FirebaseStorage.getInstance().reference
            .child("imagens_denuncias/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(novaImagemUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                storageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->

                val novaUrl = uri.toString()

                val updates = mapOf<String, Any>(

                    "/denuncias/$id/rua" to rua,
                    "/denuncias/$id/cidade" to cidade,
                    "/denuncias/$id/bairro" to bairro,
                    "/denuncias/$id/problema" to problema,
                    "/denuncias/$id/descricao" to descricao,
                    "/denuncias/$id/prefeituraDestino" to prefeituraDestino,
                    "/denuncias/$id/imagemUrl" to novaUrl,


                    "/feed/$id/cidade" to cidade,
                    "/feed/$id/problema" to problema,
                    "/feed/$id/descricao" to descricao,
                    "/feed/$id/imagemUrl" to novaUrl
                )

                rootRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Denúncia e feed atualizados com nova imagem!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
    }


}
