package com.equipoea.Tankwar.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun StartScreen(
    // El NavController nos permite movernos a otras pantallas
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "TankWar",
            style = MaterialTheme.typography.headlineLarge, // Un título grande
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Botón para iniciar el juego
        Button(
            onClick = {
                // Aquí le decimos que navegue a la ruta "game_screen"
                navController.navigate("game_screen")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "Iniciar Juego")
        }

        // Botón para guardar (aún sin funcionalidad)
        Button(
            onClick = {
                // TODO: Implementar la lógica de guardar partida
                // Por ahora no hace nada
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "Guardar Juego")
        }
    }
}