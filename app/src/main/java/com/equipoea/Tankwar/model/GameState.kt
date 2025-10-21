package com.equipoea.Tankwar.model

// Un enum para saber qué está pasando
enum class EstadoDelJuego {
    APUNTANDO,
    SIMULANDO, // El proyectil está en el aire
    EXPLOSION,
    FIN_PARTIDA
}

// Estado del mundo 2D
data class Vector2D(val x: Float, val y: Float)

// El proyectil en vuelo
data class Proyectil(
    val posicion: Vector2D,
    val velocidad: Vector2D,
    val estaVolando: Boolean = true // Siempre está volando cuando se crea
)

// Estado del tanque
data class Tanque(
    val id: Int,
    val posicion: Vector2D,
    val salud: Int = 100,
    val anguloCañon: Float = 45.0f
)

// Estado global
data class GameState(
    val tanque1: Tanque,
    val tanque2: Tanque,
    val proyectil: Proyectil? = null, // Es nulo cuando no hay nada en el aire
    val turnoActual: Int = 1, // 1 o 2
    val estadoJuego: EstadoDelJuego = EstadoDelJuego.APUNTANDO
    // Más tarde añadiremos 'terreno' y 'viento'
)