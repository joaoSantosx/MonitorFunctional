package com.jvf.monitorfunctional.ui

// Modelo de Dados simples para a lista
data class LogVideoApp(
    val titulo: String = "",
    val seguro: Boolean = true,
    val motivo: String = "",
    val timestamp: Long = 0
)
