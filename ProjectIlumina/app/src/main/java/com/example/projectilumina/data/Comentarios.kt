package com.example.projectilumina.data

data class Comentarios(
    var comentarioId: String? = null,
    var feedId: String? = null,
    var userId: String? = null,
    var nomeUsuario: String? = null,
    var fotoPerfilUrl: String? = null,
    var texto: String? = null,
    var deletada: Boolean = false,
    var dataHora: String? = null
)


