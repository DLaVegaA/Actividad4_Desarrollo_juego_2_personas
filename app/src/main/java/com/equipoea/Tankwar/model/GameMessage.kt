package com.equipoea.Tankwar.model

// Definimos los tipos de mensajes posibles
object MessageType {
    const val SHOT = "SHOT"
    const val NEXT_ROUND = "NEXT_ROUND"
    const val QUIT = "QUIT"
}

// Una sola clase para todo. Si no es un disparo, angulo y potencia se ignoran.
data class GameMessage(
    val type: String,
    val angulo: Float = 0f,
    val potencia: Float = 0f
)