package com.equipoea.Tankwar.uigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.widget.Toast
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.equipoea.Tankwar.model.Dificultad
import com.equipoea.Tankwar.model.EstadoDelJuego
import com.equipoea.Tankwar.model.GameState
import com.equipoea.Tankwar.model.ModoDeJuego
import com.equipoea.Tankwar.model.Vector2D
import com.equipoea.Tankwar.viewmodel.GameViewModel


@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modoDeJuego: ModoDeJuego,
    dificultad: Dificultad,
    isRestored: Boolean
) {
    // 'getValue' ahora se resolverá
    val state by viewModel.gameState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(true,) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(modoDeJuego, dificultad, isRestored) {
        if (!isRestored) {
            viewModel.iniciarModoDeJuego(modoDeJuego, dificultad)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                GameCanvas(state = state)
            }

            // --- LÓGICA DE CONTROLES (Corregida) ---
            // Las referencias a 'state.xxx' ahora funcionarán
            val esTurnoHumano = (state.modoDeJuego == ModoDeJuego.PVP || state.turnoActual == 1)
            val puedeControlar = (state.estadoJuego == EstadoDelJuego.APUNTANDO && esTurnoHumano)
            val puedeGuardar = (state.estadoJuego == EstadoDelJuego.APUNTANDO || state.estadoJuego == EstadoDelJuego.IA_PENSANDO)

            if (puedeControlar) {
                ControlPanel(
                    angulo = state.anguloActual,
                    potencia = state.potenciaActual,
                    turnoActual = state.turnoActual,
                    onAnguloChange = { viewModel.onAnguloChange(it) },
                    onPotenciaChange = { viewModel.onPotenciaChange(it) },
                    onDispararClick = { viewModel.onDispararClick() },
                    onSaveClick = { viewModel.onSaveGameClick() }, // <-- Corregido
                    canSave = puedeGuardar // <-- Corregido
                )
            } else if (state.estadoJuego == EstadoDelJuego.IA_PENSANDO) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "IA PENSANDO...",
                        style = MaterialTheme.typography.titleMedium,
                        // Ajusta este padding si el tamaño no coincide
                        // Esta altura es una aproximación del ControlPanel
                        modifier = Modifier.padding(vertical = 136.dp + 16.dp)
                    )
                }
            } else {
                // Añadido: Un espacio vacío para cuando se está simulando o en Game Over
                // La altura es una aproximación del ControlPanel
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    // Esta altura debe coincidir con la altura del ControlPanel
                    .height(220.dp) // Ajusta esta altura fija
                    .padding(16.dp)
                )
            }
        }

        // --- LÓGICA DE SUPERPOSICIÓN (Corregida) ---
        if (state.estadoJuego == EstadoDelJuego.FIN_PARTIDA || state.estadoJuego == EstadoDelJuego.JUEGO_TERMINADO) {
            val esJuegoTerminado = state.estadoJuego == EstadoDelJuego.JUEGO_TERMINADO
            // El ganador es el que tiene más puntos al final
            val ganador = if (state.puntuacionJ1 > state.puntuacionJ2) 1 else 2

            GameOverOverlay(
                ganador = ganador,
                esJuegoTerminado = esJuegoTerminado,
                onReiniciarClick = {
                    if (esJuegoTerminado) {
                        viewModel.onVolverAlMenuClick()
                    } else {
                        viewModel.onSiguienteRoundClick()
                    }
                }
            )
        }

        // --- Mostrar Puntuación (Corregido) ---
        ScoreDisplay(
            p1 = state.puntuacionJ1,
            p2 = state.puntuacionJ2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(top = 16.dp)
        )
    }
}

@Composable
fun ScoreDisplay(p1: Int, p2: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "J1: $p1",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = " - ",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "J2: $p2",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameCanvas(state: GameState) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scaleX = constraints.maxWidth.toFloat() / GameViewModel.MUNDO_ANCHO
        val scaleY = constraints.maxHeight.toFloat() / GameViewModel.MUNDO_ALTO
        fun scale(pos: Vector2D): Offset {
            return Offset(pos.x * scaleX, pos.y * scaleY)
        }
        fun scale(v: Float): Float {
            return v * minOf(scaleX, scaleY)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Cyan)
            val alturaSueloVm = GameViewModel.ALTURA_SUELO
            val alturaSueloPx = alturaSueloVm * scaleY
            drawRect(
                color = Color(0xFF2e7d32),
                topLeft = Offset(0f, alturaSueloPx),
                size = Size(size.width, size.height - alturaSueloPx)
            )
            drawCircle(
                color = Color.Blue,
                radius = scale(GameViewModel.RADIO_TANQUE),
                center = scale(state.tanque1.posicion)
            )
            drawCircle(
                color = Color.Red,
                radius = scale(GameViewModel.RADIO_TANQUE),
                center = scale(state.tanque2.posicion)
            )
            drawVidaBarra(
                posicion = scale(state.tanque1.posicion),
                salud = state.tanque1.salud,
                color = Color.Blue,
                radioPx = scale(GameViewModel.RADIO_TANQUE) // <-- Corregido
            )
            drawVidaBarra(
                posicion = scale(state.tanque2.posicion),
                salud = state.tanque2.salud,
                color = Color.Red,
                radioPx = scale(GameViewModel.RADIO_TANQUE) // <-- Corregido
            )
            state.proyectil?.let {
                drawCircle(
                    color = Color.Black,
                    radius = scale(GameViewModel.RADIO_PROYECTIL),
                    center = scale(it.posicion)
                )
            }
        }
    }
}

private fun DrawScope.drawVidaBarra(
    posicion: Offset,
    salud: Int,
    color: Color,
    radioPx: Float
) {
    val anchoBarraSalud = 100f
    val altoBarraSalud = 10f
    val offsetY = radioPx + 20f
    val barraPosX = posicion.x - (anchoBarraSalud / 2)
    val barraPosY = posicion.y - offsetY
    val anchoSaludActual = (anchoBarraSalud * (salud / 100f)).coerceAtLeast(0f)
    drawRect(color = Color.Gray, topLeft = Offset(barraPosX, barraPosY), size = Size(anchoBarraSalud, altoBarraSalud))
    drawRect(color = color, topLeft = Offset(barraPosX, barraPosY), size = Size(anchoSaludActual, altoBarraSalud))
    drawRect(color = Color.Black, topLeft = Offset(barraPosX, barraPosY), size = Size(anchoBarraSalud, altoBarraSalud), style = Stroke(width = 2f))
}

@Composable
fun ControlPanel(
    angulo: Float,
    potencia: Float,
    turnoActual: Int,
    onAnguloChange: (Float) -> Unit,
    onPotenciaChange: (Float) -> Unit,
    onDispararClick: () -> Unit,
    onSaveClick: () -> Unit,
    canSave: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Turno del Jugador $turnoActual", modifier = Modifier.padding(bottom = 8.dp))

        Text(text = "Ángulo: ${angulo.toInt()}°")
        Slider(
            value = angulo,
            onValueChange = onAnguloChange,
            valueRange = 0f..90f
        )

        Text(text = "Potencia: ${potencia.toInt()}")
        Slider(
            value = potencia,
            onValueChange = onPotenciaChange,
            valueRange = 10f..100f
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onDispararClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "¡DISPARAR!")
            }

            Spacer(modifier = Modifier.width(8.dp)) // <-- 'Spacer' ahora se resolverá

            Button(
                onClick = onSaveClick,
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Guardar")
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    ganador: Int,
    esJuegoTerminado: Boolean,
    onReiniciarClick: () -> Unit
) {
    val titulo = if (esJuegoTerminado) "¡FIN DEL JUEGO!" else "¡FIN DE LA RONDA!"
    val subTitulo = if (esJuegoTerminado) "Ganador del Juego: Jugador $ganador" else "Ganador de la Ronda: Jugador $ganador"
    val textoBoton = if (esJuegoTerminado) "Volver al Menú" else "Siguiente Ronda"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titulo,
            style = TextStyle(
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = subTitulo,
            style = TextStyle(
                color = if (ganador == 1) Color.Blue else Color.Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )
        Button(onClick = onReiniciarClick) {
            Text(text = textoBoton)
        }
    }
}