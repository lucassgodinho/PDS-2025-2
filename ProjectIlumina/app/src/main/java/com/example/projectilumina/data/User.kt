package com.example.projectilumina.data

data class User(
    val id: String? = null,
    var nome: String? = null,
    var email: String? = null,
    var cidade: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val token: String? = null,
    var imgperfil: String? = null
)