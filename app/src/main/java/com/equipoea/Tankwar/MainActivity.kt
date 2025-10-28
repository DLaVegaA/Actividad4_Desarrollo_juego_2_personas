package com.equipoea.Tankwar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.equipoea.Tankwar.uigame.GameScreen
import com.equipoea.Tankwar.ui.theme.TankWarTheme
import com.equipoea.Tankwar.viewmodel.GameViewModel
import androidx.navigation.compose.NavHost // <-- IMPORT AÑADIDO
import androidx.navigation.compose.composable // <-- IMPORT AÑADIDO
import androidx.navigation.compose.rememberNavController // <-- IMPORT AÑADIDO
import com.equipoea.Tankwar.ui.menu.StartScreen

class MainActivity : ComponentActivity() {

    // Inicializamos nuestro ViewModel.
    // La sintaxis 'by viewModels()' se encarga de crearlo
    // y mantenerlo vivo durante los cambios de configuración.
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge() // Para que la app ocupe toda la pantalla
        setContent {
           TankWarTheme {
               // 2. Creamos el controlador de navegación
               val navController = rememberNavController()

               // 3. Creamos el NavHost, nuestro "mapa" de pantallas
               NavHost(
                   navController = navController,
                   startDestination = "start_screen" // La primera pantalla en mostrarse
               ) {

                   // Definimos la ruta "start_screen"
                   composable(route = "start_screen") {
                       StartScreen(navController = navController)
                   }

                   // Definimos la ruta "game_screen"
                   composable(route = "game_screen") {
                       // Le pasamos el ViewModel que creamos en la Activity
                       GameScreen(viewModel = viewModel)
                   }

                   // Aquí podrías añadir más rutas, como "opciones", "créditos", etc.
               }
            }
        }
    }
}