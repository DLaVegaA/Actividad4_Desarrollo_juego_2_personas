package com.equipoea.Tankwar.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // <-- NUEVO
import androidx.compose.runtime.mutableStateOf // <-- NUEVO
import androidx.compose.runtime.remember // <-- NUEVO
import androidx.compose.runtime.setValue // <-- NUEVO
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun StartScreen(
    navController: NavController
) {
    // --- NUEVO: Estado para mostrar/ocultar los botones de dificultad ---
    var mostrarDificultad by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "TankWar",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // --- Botón Jugador vs Jugador ---
        Button(
            onClick = {
                // Navega con los argumentos PVP (Jugador vs Jugador)
                navController.navigate("game_screen/PVP/NINGUNA")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "Jugador vs Jugador")
        }

        // --- Botón Jugador vs IA ---
        Button(
            onClick = {
                // Muestra u oculta los botones de dificultad
                mostrarDificultad = !mostrarDificultad
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "Jugador vs IA")
        }

        // --- NUEVO: Botones de dificultad (se muestran condicionalmente) ---
        if (mostrarDificultad) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Botón Fácil
                Button(
                    onClick = {
                        navController.navigate("game_screen/PVE/FACIL")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = "Fácil")
                }
                // Botón Medio
                Button(
                    onClick = {
                        navController.navigate("game_screen/PVE/MEDIO")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = "Medio")
                }
                // Botón Difícil
                Button(
                    onClick = {
                        navController.navigate("game_screen/PVE/DIFICIL")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = "Difícil")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para guardar (sin cambios)
        Button(
            onClick = {
                navController.navigate("load_screen")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "Cargar Juego")
        }
    }
}