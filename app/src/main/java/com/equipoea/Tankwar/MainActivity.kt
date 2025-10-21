package com.equipoea.Tankwar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.equipoea.Tankwar.uigame.GameScreen
import com.equipoea.Tankwar.ui.theme.TankWarTheme
import com.equipoea.Tankwar.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    // Inicializamos nuestro ViewModel.
    // La sintaxis 'by viewModels()' se encarga de crearlo
    // y mantenerlo vivo durante los cambios de configuración.
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Para que la app ocupe toda la pantalla
        setContent {
           TankWarTheme {
                // ¡Aquí conectamos todo!
                // Le pasamos nuestro ViewModel a nuestra pantalla.
                GameScreen(viewModel = viewModel)
            }
        }
    }
}