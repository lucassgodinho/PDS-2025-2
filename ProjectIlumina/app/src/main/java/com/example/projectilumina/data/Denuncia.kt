package com.example.projectilumina.data
data class Denuncia(
    var id: String = "",
    var cidade: String = "",
    var bairro: String = "",
    var rua: String = "",
    var problema: String = "",
    var dataHora: String = "",
    var descricao: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var imagemUrl: String? = null,
    val userId: String = "",
    var statusColor: String = "",
    var status: String = "pendente",
    var prefeituraDestino: String? = null
)
{
    override fun toString(): String {
        return "Denuncia(id=$id, cidade='$cidade', bairro='$bairro', problema='$problema', dataHora='$dataHora', " +
                "descricao='$descricao', latitude=$latitude, longitude=$longitude, imagemUrl=$imagemUrl, " +
                "userId=$userId, status=$status, prefeitura='$prefeituraDestino' )"
    }
}