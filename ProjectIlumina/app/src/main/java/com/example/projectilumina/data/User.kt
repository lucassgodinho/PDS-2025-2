package com.example.projectilumina.data

data class User(
    val id: String? = null,
    val nome: String? = null,
    val email: String? = null,
    val cidade: String? = null,
    val latitude: Double,
    val longitude: Double ,
    val token: String? = null
){
    constructor() : this (null, null, null, null, 0.0, 0.0, null,)
    override fun toString(): String {
        return "Users(id=$id, nome='$nome', email='$email', cidade='$cidade', latitude=$latitude, longitude=$longitude, token=$token)"
    }
}