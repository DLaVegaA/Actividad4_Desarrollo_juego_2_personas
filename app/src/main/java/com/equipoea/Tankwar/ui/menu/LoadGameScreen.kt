package com.equipoea.Tankwar.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.equipoea.Tankwar.viewmodel.GameViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoadGameScreen(
    viewModel: GameViewModel,
    navController: NavController
) {
    // Observamos la lista
    val savedGames by viewModel.savedGamesList.collectAsState()
    val context = LocalContext.current

    // Carga la lista de juegos la primera vez que esta pantalla aparece
    LaunchedEffect(Unit) {
        viewModel.loadSavedGamesList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Cargar Partida",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (savedGames.isEmpty()) {
            Text(text = "No hay partidas guardadas.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(savedGames) { fileName ->
                    SavedGameItem(
                        fileName = fileName,
                        onLoadClick = {
                            val loadedState = viewModel.loadGame(fileName)
                            if (loadedState != null) {
                                Toast.makeText(context, "Partida Cargada", Toast.LENGTH_SHORT).show()
                                navController.navigate(
                                    "game_screen/${loadedState.modoDeJuego}/${loadedState.dificultad}?restored=true"
                                ) {
                                    popUpTo("start_screen")
                                }
                            }
                        },
                        // Acción de borrado ---
                        onDeleteClick = {
                            viewModel.onDeleteGameClick(fileName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedGameItem(
    fileName: String,
    onLoadClick: () -> Unit,
    onDeleteClick: () -> Unit // <-- NUEVO PARÁMETRO
) {
    // --- ACTUALIZADO: Ahora es una Fila (Row) para incluir el botón de borrado ---
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f) // La tarjeta ocupa el espacio restante
                .clickable { onLoadClick() }
        ) {
            Text(
                text = fileName.removeSuffix(".json"), // Quita la extensión
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // --- NUEVO: Botón para eliminar ---
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar Partida",
                tint = MaterialTheme.colorScheme.error // Color rojo para peligro
            )
        }
    }
}