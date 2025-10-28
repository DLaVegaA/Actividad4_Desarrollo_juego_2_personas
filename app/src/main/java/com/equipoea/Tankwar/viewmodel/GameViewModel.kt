package com.equipoea.Tankwar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipoea.Tankwar.model.EstadoDelJuego
import com.equipoea.Tankwar.model.GameState
import com.equipoea.Tankwar.model.Proyectil
import com.equipoea.Tankwar.model.Tanque
import com.equipoea.Tankwar.model.Vector2D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameViewModel : ViewModel() {

    // --- Constantes del Juego ---
    companion object {
        // --- FIX 1: Definimos los límites de nuestro mundo ---
        const val MUNDO_ANCHO = 1080f // Un ancho lógico para la pantalla
        const val MUNDO_ALTO = 1920f // Un alto lógico para la pantalla
        const val ALTURA_SUELO = 1600f // Altura Y donde está el suelo

        const val GRAVEDAD = 9.8f * 80
        const val TIEMPO_DELTA_MS = 16L // ~60 FPS
        const val TIEMPO_DELTA_S = 0.016f
        // Un valor cercano a 1.0f. Multiplicará la velocidad en cada frame.
        const val FACTOR_ROZAMIENTO = 0.995f
        const val RADIO_TANQUE = 30f
        const val RADIO_PROYECTIL = 15f
        const val DISTANCIA_IMPACTO = RADIO_TANQUE + RADIO_PROYECTIL // 45f
        const val DANO_PROYECTIL = 25 // Daño que hace un impacto
    }

    // Estado privado y mutable
    private val _gameState = MutableStateFlow(crearEstadoInicial())

    // Estado público de solo lectura para la UI
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // --- Variables para los Controles de la UI ---
    private val _anguloActual = MutableStateFlow(45.0f)
    val anguloActual: StateFlow<Float> = _anguloActual.asStateFlow()

    private val _potenciaActual = MutableStateFlow(50.0f)
    val potenciaActual: StateFlow<Float> = _potenciaActual.asStateFlow()


    // Función para inicializar el juego
    private fun crearEstadoInicial(): GameState {
        // Posicionamos los tanques dentro del MUNDO_ANCHO
        val tanque1 = Tanque(id = 1, posicion = Vector2D(100f, ALTURA_SUELO - RADIO_TANQUE))
        val tanque2 = Tanque(id = 2, posicion = Vector2D(MUNDO_ANCHO - 100f, ALTURA_SUELO - RADIO_TANQUE))

        return GameState(
            tanque1 = tanque1,
            tanque2 = tanque2,
            proyectil = null,
            turnoActual = 1,
            estadoJuego = EstadoDelJuego.APUNTANDO
        )
    }

    // --- Funciones llamadas por la UI ---

    fun onAnguloChange(nuevoAngulo: Float) {
        _anguloActual.value = nuevoAngulo
    }

    fun onPotenciaChange(nuevaPotencia: Float) {
        _potenciaActual.value = nuevaPotencia
    }

    fun onDispararClick() {
        if (_gameState.value.estadoJuego != EstadoDelJuego.APUNTANDO) return

        viewModelScope.launch(Dispatchers.Default) {
            iniciarSimulacion()
        }
    }

    fun onReiniciarClick() {
        _gameState.value = crearEstadoInicial()
        _anguloActual.value = 45.0f
        _potenciaActual.value = 50.0f
    }

    // --- El Motor de Física ---

    private suspend fun iniciarSimulacion() {
        _gameState.update { it.copy(estadoJuego = EstadoDelJuego.SIMULANDO) }

        val angulo = _anguloActual.value
        val potencia = _potenciaActual.value * 15

        var proyectilActual = crearProyectilInicial(angulo, potencia)
        var huboImpacto = false

        while (proyectilActual.estaVolando) {

            // --- A. Aplicar Física ---
            val velocidadYAnterior = proyectilActual.velocidad.y
            val velocidadXAnterior = proyectilActual.velocidad.x

            // Aplicamos gravedad Y resistencia del aire
            val nuevaVelocidadY = (velocidadYAnterior + (GRAVEDAD * TIEMPO_DELTA_S)) * FACTOR_ROZAMIENTO
            // Aplicamos resistencia del aire
            val nuevaVelocidadX = velocidadXAnterior * FACTOR_ROZAMIENTO

            val nuevaPosX = proyectilActual.posicion.x + (nuevaVelocidadX * TIEMPO_DELTA_S)
            val nuevaPosY = proyectilActual.posicion.y + (nuevaVelocidadY * TIEMPO_DELTA_S)

            proyectilActual = proyectilActual.copy(
                posicion = Vector2D(nuevaPosX, nuevaPosY),
                velocidad = Vector2D(nuevaVelocidadX, nuevaVelocidadY)
            )

            // --- B. Actualizar el Estado Global ---
            _gameState.update { it.copy(proyectil = proyectilActual) }

            // --- C. Comprobar Colisiones ---
            val estadoActual = _gameState.value
            val tanqueEnemigo = if (estadoActual.turnoActual == 1) estadoActual.tanque2 else estadoActual.tanque1
            val distancia = calcularDistancia(proyectilActual.posicion, tanqueEnemigo.posicion)

            if (distancia < DISTANCIA_IMPACTO) {
                huboImpacto = true
                proyectilActual = proyectilActual.copy(estaVolando = false) // Detiene el proyectil
            }

            // --- FIX 1: Comprobación de límites (Bounds Check) ---
            val x = proyectilActual.posicion.x
            val y = proyectilActual.posicion.y

            val chocaSuelo = y > ALTURA_SUELO
            val chocaTecho = y < 0f
            val chocaParedIzquierda = x < 0f
            val chocaParedDerecha = x > MUNDO_ANCHO
            // TODO: Añadir colisión con tanques aquí

            if (chocaSuelo || chocaTecho || chocaParedIzquierda || chocaParedDerecha) {
                proyectilActual = proyectilActual.copy(estaVolando = false)

                if (chocaSuelo) Log.d("GameViewModel", "¡Impacto en el suelo!")
                //else Log.d("GameViewModel", "¡Proyectil fuera de límites!")
            }
            // --- Fin del FIX 1 ---

            // --- D. Esperar al siguiente frame ---
            if (proyectilActual.estaVolando) {
                delay(TIEMPO_DELTA_MS)
            }
        }

        // --- 4. Fin de la Simulación ---
        Log.d("GameViewModel", "Simulación terminada.")

        _gameState.update { it.copy(
            estadoJuego = EstadoDelJuego.EXPLOSION,
            proyectil = null // Ocultamos el proyectil
        )}
        delay(1000)

        // --- NUEVO: Aplicar daño y comprobar fin de partida ---
        var esFinDePartida = false
        if (huboImpacto) {
            _gameState.update { estadoActual ->
                val tanqueEnemigoId = if (estadoActual.turnoActual == 1) 2 else 1
                val tanqueEnemigo = if (tanqueEnemigoId == 1) estadoActual.tanque1 else estadoActual.tanque2

                val nuevaSalud = (tanqueEnemigo.salud - DANO_PROYECTIL).coerceAtLeast(0)
                Log.d("GameViewModel", "¡Impacto en Tanque $tanqueEnemigoId! Salud restante: $nuevaSalud")

                esFinDePartida = (nuevaSalud == 0)
                val nuevoEstadoJuego = if (esFinDePartida) EstadoDelJuego.FIN_PARTIDA else EstadoDelJuego.APUNTANDO

                if (tanqueEnemigoId == 1) {
                    estadoActual.copy(tanque1 = tanqueEnemigo.copy(salud = nuevaSalud), estadoJuego = nuevoEstadoJuego)
                } else {
                    estadoActual.copy(tanque2 = tanqueEnemigo.copy(salud = nuevaSalud), estadoJuego = nuevoEstadoJuego)
                }
            }
        }

        // Solo cambiamos de turno si el juego NO ha terminado
        if (!esFinDePartida && !huboImpacto) {
            // Si no hubo impacto, solo cambiamos el turno
            cambiarTurno()
        } else if (esFinDePartida) {
            Log.d("GameViewModel", "¡FIN DE LA PARTIDA!")
            // No hacemos nada, el estado FIN_PARTIDA bloqueará los controles
        } else if (huboImpacto && !esFinDePartida) {
            // Si hubo impacto PERO no es fin de partida, cambiamos el turno
            cambiarTurno()
        }

        //cambiarTurno()
    }

    private fun crearProyectilInicial(angulo: Float, potencia: Float): Proyectil {
        val estado = _gameState.value
        val tanqueActual = if (estado.turnoActual == 1) estado.tanque1 else estado.tanque2

        val anguloRad = Math.toRadians(angulo.toDouble()).toFloat()

        // --- FIX 2: Calcular dirección horizontal basada en el jugador ---
        val direccion = if (estado.turnoActual == 1) 1f else -1f
        val vx = potencia * cos(anguloRad) * direccion // Multiplicamos por la dirección
        // --- Fin del FIX 2 ---

        val vy = -potencia * sin(anguloRad) // 'Y' es negativo para ir "hacia arriba"

        return Proyectil(
            posicion = tanqueActual.posicion,
            velocidad = Vector2D(vx, vy),
            estaVolando = true
        )
    }

    private fun cambiarTurno() {
        _gameState.update { estadoActual ->
            estadoActual.copy(
                estadoJuego = EstadoDelJuego.APUNTANDO,
                turnoActual = if (estadoActual.turnoActual == 1) 2 else 1
            )
        }
        _anguloActual.value = 45.0f
        _potenciaActual.value = 50.0f
    }

    private fun calcularDistancia(p1: Vector2D, p2: Vector2D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
}