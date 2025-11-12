package com.equipoea.Tankwar.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.equipoea.Tankwar.data.GameRepository
import com.equipoea.Tankwar.model.Dificultad
import com.equipoea.Tankwar.model.EstadoDelJuego
import com.equipoea.Tankwar.model.GameState
import com.equipoea.Tankwar.model.ModoDeJuego
import com.equipoea.Tankwar.model.Proyectil
import com.equipoea.Tankwar.model.Tanque
import com.equipoea.Tankwar.model.Vector2D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import com.equipoea.Tankwar.data.SettingsManager
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted


// Hereda de AndroidViewModel para obtener el Context
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // --- Constantes del Juego ---
    companion object {
        const val MUNDO_ANCHO = 1080f
        const val MUNDO_ALTO = 1920f
        const val ALTURA_SUELO = 1600f
        const val GRAVEDAD = 9.8f * 80
        const val TIEMPO_DELTA_MS = 16L
        const val TIEMPO_DELTA_S = 0.016f
        const val FACTOR_ROZAMIENTO = 0.995f
        const val RADIO_TANQUE = 30f
        const val RADIO_PROYECTIL = 15f
        const val DISTANCIA_IMPACTO = RADIO_TANQUE + RADIO_PROYECTIL
        const val DANO_PROYECTIL = 25
        const val PUNTOS_PARA_GANAR = 3
    }

    // --- Instancia del Repositorio ---
    private val repository = GameRepository(application.applicationContext)

    private val settingsManager = SettingsManager(application)

    // --- ESTADO (Versión única y limpia) ---
    private val _gameState = MutableStateFlow(crearEstadoInicial())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _navegarAInicio = MutableSharedFlow<Unit>()
    val navegarAInicio = _navegarAInicio.asSharedFlow()

    private val _savedGamesList = MutableStateFlow<List<String>>(emptyList())

    val savedGamesList: StateFlow<List<String>> = _savedGamesList.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    val isDarkTheme: StateFlow<Boolean> = settingsManager.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(isDark)
        }
    }

    // --- Funciones de Configuración ---
    fun iniciarModoDeJuego(modo: ModoDeJuego, dificultad: Dificultad) {
        _gameState.value = crearEstadoInicial(modo, dificultad)
    }

    private fun crearEstadoInicial(
        modo: ModoDeJuego = ModoDeJuego.PVP,
        dificultad: Dificultad = Dificultad.NINGUNA
    ): GameState {
        val tanque1 = Tanque(id = 1, posicion = Vector2D(100f, ALTURA_SUELO - RADIO_TANQUE), salud = 100)
        val tanque2 = Tanque(id = 2, posicion = Vector2D(MUNDO_ANCHO - 100f, ALTURA_SUELO - RADIO_TANQUE), salud = 100)
        return GameState(
            tanque1 = tanque1,
            tanque2 = tanque2,
            proyectil = null,
            turnoActual = 1,
            estadoJuego = EstadoDelJuego.APUNTANDO,
            puntuacionJ1 = 0,
            puntuacionJ2 = 0,
            modoDeJuego = modo,
            dificultad = dificultad,
            loadedFromFileName = null,
            anguloActual = 45.0f,
            potenciaActual = 50.0f
        )
    }

    // Funciones de UI ---
    fun onAnguloChange(nuevoAngulo: Float) {
        _gameState.update { it.copy(anguloActual = nuevoAngulo) }
    }

    fun onPotenciaChange(nuevaPotencia: Float) {
        _gameState.update { it.copy(potenciaActual = nuevaPotencia) }
    }

    fun onDispararClick() {
        if (_gameState.value.estadoJuego != EstadoDelJuego.APUNTANDO &&
            _gameState.value.estadoJuego != EstadoDelJuego.IA_PENSANDO) return

        viewModelScope.launch(Dispatchers.Default) {
            iniciarSimulacion()
        }
    }

    fun onSiguienteRoundClick() {
        _gameState.update { estadoActual ->
            estadoActual.copy(
                tanque1 = estadoActual.tanque1.copy(salud = 100, posicion = Vector2D(100f, ALTURA_SUELO - RADIO_TANQUE)),
                tanque2 = estadoActual.tanque2.copy(salud = 100, posicion = Vector2D(MUNDO_ANCHO - 100f, ALTURA_SUELO - RADIO_TANQUE)),
                estadoJuego = EstadoDelJuego.APUNTANDO,
                turnoActual = 1,
                anguloActual = 45.0f, // Resetea ángulo
                potenciaActual = 50.0f // Resetea potencia
            )
        }
    }

    fun onVolverAlMenuClick() {
        _gameState.value = crearEstadoInicial()
        viewModelScope.launch {
            _navegarAInicio.emit(Unit)
        }
    }

    // --- Funciones de Guardado/Carga ---
    fun onSaveGameClick() {
        val estado = _gameState.value
        if (estado.estadoJuego != EstadoDelJuego.APUNTANDO &&
            estado.estadoJuego != EstadoDelJuego.IA_PENSANDO) {
            Log.w("GameViewModel", "No se puede guardar ahora.")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            //Revisa si ya tenemos un nombre de archivo
            val baseFileName = if (estado.loadedFromFileName != null) {
                //Sí, sobrescribe
                estado.loadedFromFileName.removeSuffix(".json")
            }else{
                //No, crea uno nuevo
                "partida_${System.currentTimeMillis()}"
            }
            //Guarda el estado actual en el archivo
            repository.saveGame(estado, baseFileName)

            //Actualiza el estado en memoria para "recordar" el archivo guardado
            _gameState.update { it.copy(loadedFromFileName = "$baseFileName.json") }
            _uiEvent.emit("Partida Guardada")
        }
    }

    fun getSavedGamesList(): List<String> {
        return repository.getSavedGamesList()
    }

    // Esta función ahora actualiza el StateFlow
    fun loadSavedGamesList() {
        _savedGamesList.value = repository.getSavedGamesList()
    }

    fun loadGame(fileName: String): GameState? {
        val loadedState = repository.loadGame(fileName)
        if (loadedState != null) {
            //Actualiza el estado con el juego cargado
            _gameState.value = loadedState.copy(loadedFromFileName = fileName)
        }
        return loadedState
    }

    fun onDeleteGameClick(fileName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            // 1. Borra el archivo
            repository.deleteGame(fileName)
            // 2. Actualiza la lista para que la UI reaccione
            loadSavedGamesList()
        }
    }

    // --- Motor de Física y Simulación ---
    private suspend fun iniciarSimulacion() {
        _gameState.update { it.copy(estadoJuego = EstadoDelJuego.SIMULANDO) }
        val angulo = _gameState.value.anguloActual
        val potencia = _gameState.value.potenciaActual * 15
        var proyectilActual = crearProyectilInicial(angulo, potencia)
        var huboImpacto = false

        while (proyectilActual.estaVolando) {
            val velocidadYAnterior = proyectilActual.velocidad.y
            val velocidadXAnterior = proyectilActual.velocidad.x

            val nuevaVelocidadY = (velocidadYAnterior + (GRAVEDAD * TIEMPO_DELTA_S)) * FACTOR_ROZAMIENTO
            val nuevaVelocidadX = velocidadXAnterior * FACTOR_ROZAMIENTO

            val nuevaPosX = proyectilActual.posicion.x + (nuevaVelocidadX * TIEMPO_DELTA_S)
            val nuevaPosY = proyectilActual.posicion.y + (nuevaVelocidadY * TIEMPO_DELTA_S)

            proyectilActual = proyectilActual.copy(
                posicion = Vector2D(nuevaPosX, nuevaPosY),
                velocidad = Vector2D(nuevaVelocidadX, nuevaVelocidadY)
            )
            _gameState.update { it.copy(proyectil = proyectilActual) }

            val estadoActual = _gameState.value
            val tanqueEnemigo = if (estadoActual.turnoActual == 1) estadoActual.tanque2 else estadoActual.tanque1

            val distancia = calcularDistancia(proyectilActual.posicion, tanqueEnemigo.posicion)

            if (distancia < DISTANCIA_IMPACTO) {
                huboImpacto = true
                proyectilActual = proyectilActual.copy(estaVolando = false)
            }

            val x = proyectilActual.posicion.x
            val y = proyectilActual.posicion.y
            val chocaSuelo = y > ALTURA_SUELO
            val chocaTecho = y < 0f
            val chocaParedIzquierda = x < 0f
            val chocaParedDerecha = x > MUNDO_ANCHO

            if (chocaSuelo || chocaTecho || chocaParedIzquierda || chocaParedDerecha) {
                proyectilActual = proyectilActual.copy(estaVolando = false)
                if (chocaSuelo) Log.d("GameViewModel", "¡Impacto en el suelo!")
            }

            if (proyectilActual.estaVolando) {
                delay(TIEMPO_DELTA_MS)
            }
        }
        _gameState.update { it.copy(estadoJuego = EstadoDelJuego.EXPLOSION, proyectil = null) }
        delay(1000)

        // --- Lógica de Puntuación (Corregida) ---
        var esFinDeRound = false
        var esFinDeJuego = false
        if (huboImpacto) {
            _gameState.update { estadoActual ->
                val tanqueEnemigoId = if (estadoActual.turnoActual == 1) 2 else 1
                val tanqueEnemigo = if (tanqueEnemigoId == 1) estadoActual.tanque1 else estadoActual.tanque2
                val nuevaSalud = (tanqueEnemigo.salud - DANO_PROYECTIL).coerceAtLeast(0)

                var nuevaPuntuacionJ1 = estadoActual.puntuacionJ1
                var nuevaPuntuacionJ2 = estadoActual.puntuacionJ2
                var nuevoEstadoJuego: EstadoDelJuego

                if (nuevaSalud == 0) {
                    esFinDeRound = true
                    val ganadorRonda = estadoActual.turnoActual
                    if (ganadorRonda == 1) {
                        nuevaPuntuacionJ1++
                        if (nuevaPuntuacionJ1 == PUNTOS_PARA_GANAR) esFinDeJuego = true
                    } else {
                        nuevaPuntuacionJ2++
                        if (nuevaPuntuacionJ2 == PUNTOS_PARA_GANAR) esFinDeJuego = true
                    }
                    nuevoEstadoJuego = if (esFinDeJuego) EstadoDelJuego.JUEGO_TERMINADO else EstadoDelJuego.FIN_PARTIDA
                } else {
                    nuevoEstadoJuego = EstadoDelJuego.APUNTANDO
                }

                if (tanqueEnemigoId == 1) {
                    estadoActual.copy(
                        tanque1 = estadoActual.tanque1.copy(salud = nuevaSalud),
                        estadoJuego = nuevoEstadoJuego,
                        puntuacionJ1 = nuevaPuntuacionJ1,
                        puntuacionJ2 = nuevaPuntuacionJ2
                    )
                } else {
                    estadoActual.copy(
                        tanque2 = estadoActual.tanque2.copy(salud = nuevaSalud),
                        estadoJuego = nuevoEstadoJuego,
                        puntuacionJ1 = nuevaPuntuacionJ1,
                        puntuacionJ2 = nuevaPuntuacionJ2
                    )
                }
            }
        }

        if (!esFinDeRound && !esFinDeJuego) {
            cambiarTurno()
        }
    }

    private fun crearProyectilInicial(angulo: Float, potencia: Float): Proyectil {
        val estado = _gameState.value
        val tanqueActual = if (estado.turnoActual == 1) estado.tanque1 else estado.tanque2
        val anguloRad = Math.toRadians(angulo.toDouble()).toFloat()
        val direccion = if (estado.turnoActual == 1) 1f else -1f
        val vx = potencia * cos(anguloRad) * direccion
        val vy = -potencia * sin(anguloRad)
        return Proyectil(
            posicion = tanqueActual.posicion,
            velocidad = Vector2D(vx, vy),
            estaVolando = true
        )
    }

    private fun cambiarTurno() {
        var proximoTurno = 1
        _gameState.update { estadoActual ->
            proximoTurno = if (estadoActual.turnoActual == 1) 2 else 1
            if (estadoActual.modoDeJuego == ModoDeJuego.PVE && proximoTurno == 2) {
                estadoActual.copy(
                    estadoJuego = EstadoDelJuego.IA_PENSANDO,
                    turnoActual = 2
                )
            } else {
                estadoActual.copy(
                    estadoJuego = EstadoDelJuego.APUNTANDO,
                    turnoActual = proximoTurno,
                    anguloActual = 45.0f, // Resetea para el jugador humano
                    potenciaActual = 50.0f
                )
            }
        }
        if (_gameState.value.estadoJuego == EstadoDelJuego.IA_PENSANDO) {
            ejecutarTurnoIA()
        }
    }

    // --- FUNCIÓN RESTAURADA ---
    private fun calcularDistancia(p1: Vector2D, p2: Vector2D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    // --- CEREBRO DE LA IA ---
    private fun ejecutarTurnoIA() {
        viewModelScope.launch(Dispatchers.Default) {
            delay(1500)
            val estado = _gameState.value
            val (anguloIA, potenciaIA) = calcularDisparoIA(
                estado.dificultad,
                estado.tanque2,
                estado.tanque1
            )
            // Actualiza el estado con el disparo de la IA
            _gameState.update { it.copy(anguloActual = anguloIA, potenciaActual = potenciaIA) }
            delay(500)
            onDispararClick()
        }
    }

    private fun calcularDisparoIA(
        dificultad: Dificultad,
        tanqueIA: Tanque,
        tanqueHumano: Tanque
    ): Pair<Float, Float> {
        val anguloPerfecto = 45f
        val potenciaPerfecta = 65f

        return when (dificultad) {
            Dificultad.FACIL -> {
                val angulo = Random.nextDouble(20.0, 70.0).toFloat()
                val potencia = Random.nextDouble(30.0, 80.0).toFloat()
                Pair(angulo, potencia)
            }
            Dificultad.MEDIO -> {
                val anguloError = Random.nextDouble(-15.0, 15.0).toFloat()
                val potenciaError = Random.nextDouble(-15.0, 15.0).toFloat()
                val angulo = (anguloPerfecto + anguloError).coerceIn(10f, 80f)
                val potencia = (potenciaPerfecta + potenciaError).coerceIn(30f, 100f)
                Pair(angulo, potencia)
            }
            Dificultad.DIFICIL -> {
                val chance = Random.nextInt(1, 101)
                if (chance <= 20) {
                    val anguloError = Random.nextDouble(-10.0, 10.0).toFloat()
                    val potenciaError = Random.nextDouble(-10.0, 10.0).toFloat()
                    val angulo = (anguloPerfecto + anguloError).coerceIn(30f, 60f)
                    val potencia = (potenciaPerfecta + potenciaError).coerceIn(50f, 100f)
                    Log.d("GameViewModel_IA", "IA Difícil: FALLO INTENCIONAL")
                    Pair(angulo, potencia)
                } else {
                    val anguloError = Random.nextDouble(-1.5, 1.5).toFloat()
                    val potenciaError = Random.nextDouble(-1.5, 1.5).toFloat()
                    val angulo = (anguloPerfecto + anguloError)
                    val potencia = (potenciaPerfecta + potenciaError)
                    Log.d("GameViewModel_IA", "IA Difícil: ACIERTO")
                    Pair(angulo, potencia)
                }
            }
            Dificultad.NINGUNA -> Pair(45f, 50f)
        }
    }
}
