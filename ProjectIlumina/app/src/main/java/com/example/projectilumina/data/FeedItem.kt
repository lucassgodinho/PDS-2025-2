package com.example.projectilumina.data

data class FeedItem(
    var id: String? = null,
    var denunciaId: String? = null,
    var userId: String? = null,
    var problema: String? = null,
    var descricao: String? = null,
    var imagemUrl: String? = null,

    var comentarioId: String? = null,
    var comentario: String? = null,

    var deletada: Boolean = false,
    var curtidas: Int = 0,

    var nomeUsuario: String? = null,
    var cidade: String? = null,
    var fotoPerfilUrl: String? = null,

    var likes: MutableMap<String, Boolean>? = null
)
