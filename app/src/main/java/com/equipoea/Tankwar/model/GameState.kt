package com.equipoea.Tankwar.model

// --- NUEVO: Enum para el modo de juego ---
enum class ModoDeJuego {
    PVP, // Jugador vs Jugador
    PVE  // Jugador vs IA
}

// --- NUEVO: Enum para la dificultad ---
enum class Dificultad {
    NINGUNA,
    FACIL,
    MEDIO,
    DIFICIL
}

// Un enum para saber qué está pasando
enum class EstadoDelJuego {
    APUNTANDO,
    SIMULANDO, // El proyectil está en el aire
    EXPLOSION,
    FIN_PARTIDA, // Fin de una ronda
    JUEGO_TERMINADO, // Fin del juego (puntuación = 3)
    IA_PENSANDO // <-- NUEVO: Turno de la IA
}

// Estado del mundo 2D
data class Vector2D(val x: Float, val y: Float)

// El proyectil en vuelo
data class Proyectil(
    val posicion: Vector2D,
    val velocidad: Vector2D,
    val estaVolando: Boolean = true
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
    val proyectil: Proyectil? = null,
    val turnoActual: Int = 1,
    val estadoJuego: EstadoDelJuego = EstadoDelJuego.APUNTANDO,
    val puntuacionJ1: Int = 0,
    val puntuacionJ2: Int = 0,

    // --- NUEVO: Propiedades del modo de juego ---
    val modoDeJuego: ModoDeJuego = ModoDeJuego.PVP,
    val dificultad: Dificultad = Dificultad.NINGUNA,
    val anguloActual: Float = 45.0f,
    val potenciaActual: Float = 50.0f,
    val loadedFromFileName: String? = null
)