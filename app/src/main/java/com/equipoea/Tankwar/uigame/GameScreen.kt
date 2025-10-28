package com.equipoea.Tankwar.uigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.equipoea.Tankwar.model.EstadoDelJuego
import com.equipoea.Tankwar.model.GameState
import com.equipoea.Tankwar.model.Vector2D
import com.equipoea.Tankwar.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.gameState.collectAsState()
    val angulo by viewModel.anguloActual.collectAsState()
    val potencia by viewModel.potenciaActual.collectAsState()

    // --- EL LAYOUT FINAL Y CORRECTO ---
    // Un Box permite que el GameOverOverlay se superponga
    Box(modifier = Modifier.fillMaxSize()) {

        // Esta Columna organiza la pantalla: Juego arriba, Controles abajo
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Canvas (ocupa el espacio restante)
            Box(modifier = Modifier
                .weight(1f) // <-- Ocupa todo el espacio MENOS los controles
                .fillMaxWidth()) {

                // Este Canvas ahora escala su contenido
                GameCanvas(state = state)
            }

            // 2. Controles (en la parte de abajo)
            if (state.estadoJuego == EstadoDelJuego.APUNTANDO) {
                ControlPanel(
                    angulo = angulo,
                    potencia = potencia,
                    turnoActual = state.turnoActual,
                    onAnguloChange = { viewModel.onAnguloChange(it) },
                    onPotenciaChange = { viewModel.onPotenciaChange(it) },
                    onDispararClick = { viewModel.onDispararClick() }
                )
            }
        }

        // 3. Pantalla de Fin de Partida (Superpuesta)
        if (state.estadoJuego == EstadoDelJuego.FIN_PARTIDA) {
            val ganador = if (state.tanque1.salud > 0) 1 else 2
            GameOverOverlay(
                ganador = ganador,
                onReiniciarClick = { viewModel.onReiniciarClick() }
            )
        }
    }
}

@Composable
fun GameCanvas(state: GameState) {
    // --- ESTA ES LA MAGIA ---
    // Usamos BoxWithConstraints para saber el tamaño real del Canvas
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // constraints.maxWidth y constraints.maxHeight son el tamaño real
        // de este Box (el que tiene weight(1f))

        // Calculamos los factores de escala
        val scaleX = constraints.maxWidth.toFloat() / GameViewModel.MUNDO_ANCHO
        val scaleY = constraints.maxHeight.toFloat() / GameViewModel.MUNDO_ALTO

        // Creamos una función de transformación para Coordenadas
        fun scale(pos: Vector2D): Offset {
            return Offset(pos.x * scaleX, pos.y * scaleY)
        }

        // Creamos una función de transformación para Radios/Distancias
        fun scale(v: Float): Float {
            // Usamos el promedio para los radios, o el más pequeño
            return v * minOf(scaleX, scaleY)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // El mundo del ViewModel es 1080x1920
            // El mundo de este Canvas es size.width x size.height (ej: 1080x1700)

            // 1. Dibujar el fondo
            drawRect(color = Color.Cyan)

            // 2. Dibujar el suelo
            val alturaSueloVm = GameViewModel.ALTURA_SUELO // 1600f (en mundo ViewModel)
            val alturaSueloPx = alturaSueloVm * scaleY // (en mundo Canvas)
            drawRect(
                color = Color(0xFF2e7d32),
                topLeft = Offset(0f, alturaSueloPx),
                size = Size(size.width, size.height - alturaSueloPx)
            )

            // 3. Dibujar Tanque 1
            drawCircle(
                color = Color.Blue,
                radius = scale(GameViewModel.RADIO_TANQUE), // <-- Se escala
                center = scale(state.tanque1.posicion) // <-- Se escala
            )

            // 4. Dibujar Tanque 2
            drawCircle(
                color = Color.Red,
                radius = scale(GameViewModel.RADIO_TANQUE), // <-- Se escala
                center = scale(state.tanque2.posicion) // <-- Se escala
            )

            // 5. Dibujar Barras de Vida
            drawVidaBarra(
                posicion = scale(state.tanque1.posicion),
                salud = state.tanque1.salud,
                color = Color.Blue,
                radioPx = scale(GameViewModel.RADIO_TANQUE)
            )
            drawVidaBarra(
                posicion = scale(state.tanque2.posicion),
                salud = state.tanque2.salud,
                color = Color.Red,
                radioPx = scale(GameViewModel.RADIO_TANQUE)
            )

            // 6. Dibujar Proyectil
            state.proyectil?.let {
                drawCircle(
                    color = Color.Black,
                    radius = scale(GameViewModel.RADIO_PROYECTIL), // <-- Se escala
                    center = scale(it.posicion) // <-- Se escala
                )
            }
        }
    }
}

// --- NUEVO: Función de ayuda para dibujar la vida ---
// Actualizamos drawVidaBarra para que use píxeles escalados
private fun DrawScope.drawVidaBarra(
    posicion: Offset, // Ya está escalado
    salud: Int,
    color: Color,
    radioPx: Float // Ya está escalado
) {
    val anchoBarraSalud = 100f // Mantenemos este ancho en Px
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
    onDispararClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Color de fondo del tema
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Turno del Jugador $turnoActual", modifier = Modifier.padding(bottom = 8.dp))

        // Slider de Ángulo
        Text(text = "Ángulo: ${angulo.toInt()}°")
        Slider(
            value = angulo,
            onValueChange = onAnguloChange,
            valueRange = 0f..90f // Ángulo de 0 a 90 grados
        )

        // Slider de Potencia
        Text(text = "Potencia: ${potencia.toInt()}")
        Slider(
            value = potencia,
            onValueChange = onPotenciaChange,
            valueRange = 10f..100f // Potencia de 10 a 100
        )

        // Botón de Disparo
        Button(
            onClick = onDispararClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = "¡DISPARAR!")
        }
    }
}

// --- NUEVO: Composable para la pantalla de Fin de Partida ---
@Composable
fun GameOverOverlay(ganador: Int, onReiniciarClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)) // Fondo oscuro semitransparente
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡FIN DE LA PARTIDA!",
            style = TextStyle(
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Ganador: Jugador $ganador",
            style = TextStyle(
                color = if (ganador == 1) Color.Blue else Color.Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )
        Button(onClick = onReiniciarClick) {
            Text(text = "Jugar de Nuevo")
        }
    }
}