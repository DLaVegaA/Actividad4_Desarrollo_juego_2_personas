package com.equipoea.Tankwar.uigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.equipoea.Tankwar.model.EstadoDelJuego
import com.equipoea.Tankwar.model.GameState
import com.equipoea.Tankwar.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel) {

    // Observamos el GameState completo
    val state by viewModel.gameState.collectAsState()

    // Observamos los valores de los sliders
    val angulo by viewModel.anguloActual.collectAsState()
    val potencia by viewModel.potenciaActual.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // --- 1. Lienzo del Juego (ocupa todo el espacio posible) ---
        Box(modifier = Modifier.weight(1f)) {
            GameCanvas(state = state)
        }

        // --- 2. Panel de Controles ---
        // Solo mostramos los controles si estamos en la fase de APUNTANDO
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
}

@Composable
fun GameCanvas(state: GameState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 'this' es el DrawScope

        // 1. Dibujar el fondo
        drawRect(color = Color.Cyan)

        // --- NUEVO: Dibujar el suelo basado en la lógica ---
        // Usamos la constante del ViewModel (importada)
        val alturaSueloPx = GameViewModel.ALTURA_SUELO

        drawRect(
            color = Color(0xFF2e7d32), // Un verde oscuro
            topLeft = Offset(0f, alturaSueloPx),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - alturaSueloPx)
        )

        // 3. Dibujar el Tanque 1 (basado en el estado)
        // Su 'Y' ya está calculada en el ViewModel (ALTURA_SUELO - 30f)
        drawCircle(
            color = Color.Blue,
            radius = 30f,
            center = Offset(state.tanque1.posicion.x, state.tanque1.posicion.y)
        )

        // 4. Dibujar el Tanque 2 (basado en el estado)
        drawCircle(
            color = Color.Red,
            radius = 30f,
            center = Offset(state.tanque2.posicion.x, state.tanque2.posicion.y)
        )

        // 5. Dibujar el Proyectil (si existe)
        state.proyectil?.let {
            drawCircle(
                color = Color.Black,
                radius = 15f,
                center = Offset(it.posicion.x, it.posicion.y)
            )
        }
    }
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