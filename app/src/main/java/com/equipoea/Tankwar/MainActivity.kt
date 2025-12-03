package com.equipoea.Tankwar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.equipoea.Tankwar.uigame.GameScreen
import com.equipoea.Tankwar.ui.theme.TankWarTheme
import com.equipoea.Tankwar.viewmodel.GameViewModel
import com.equipoea.Tankwar.viewmodel.BluetoothViewModel // <-- AÑADIDO
import com.equipoea.Tankwar.ui.menu.BluetoothLobbyScreen // <-- AÑADIDO
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.equipoea.Tankwar.ui.menu.StartScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavType // <-- IMPORT AÑADIDO
import androidx.navigation.navArgument // <-- IMPORT AÑADIDO
import com.equipoea.Tankwar.model.Dificultad
import com.equipoea.Tankwar.model.ModoDeJuego
import com.equipoea.Tankwar.ui.menu.LoadGameScreen // <-- IMPORT AÑADIDO
import kotlinx.coroutines.flow.collect // <-- IMPORT AÑADIDO

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels() // <-- AÑADIDO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TankWarTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    viewModel.navegarAInicio.collect { // <-- 'collect' ahora se resolverá
                        Log.d("MainActivity", "Recibido evento para navegar a inicio.")
                        navController.popBackStack("start_screen", inclusive = false)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "start_screen"
                ) {
                    composable(route = "start_screen") {
                        StartScreen(navController = navController)
                    }

                    // --- RUTA MODIFICADA ---
                    composable(
                        route = "game_screen/{modoDeJuego}/{dificultad}?restored={restored}&localPlayerId={localPlayerId}", // <-- PARÁMETRO AÑADIDO
                        arguments = listOf(
                            navArgument("modoDeJuego") { type = NavType.StringType },
                            navArgument("dificultad") { type = NavType.StringType },
                            navArgument("restored") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                            navArgument("localPlayerId") { // <-- ARGUMENTO AÑADIDO
                                type = NavType.IntType
                                defaultValue = 1 // Default a Jugador 1 (Host)
                            }
                        )
                    ) { backStackEntry ->
                        val modoStr = backStackEntry.arguments?.getString("modoDeJuego") ?: "PVP"
                        val dificultadStr = backStackEntry.arguments?.getString("dificultad") ?: "NINGUNA"
                        val modo = ModoDeJuego.valueOf(modoStr)
                        val dificultad = Dificultad.valueOf(dificultadStr)
                        val restored = backStackEntry.arguments?.getBoolean("restored") ?: false
                        val localPlayerId = backStackEntry.arguments?.getInt("localPlayerId") ?: 1 // <-- VALOR AÑADIDO

                        GameScreen(
                            viewModel = viewModel,
                            modoDeJuego = modo,
                            dificultad = dificultad,
                            isRestored = restored,
                            localPlayerId = localPlayerId // <-- PASAR VALOR
                        )
                    }

                    composable(route = "load_screen") {
                        LoadGameScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }

                    // --- RUTA AÑADIDA ---
                    composable(route = "bluetooth_lobby_screen") {
                        BluetoothLobbyScreen(
                            navController = navController,
                            viewModel = bluetoothViewModel
                        )
                    }
                }
            }
        }
    }

    // <-- AÑADIDO: Detener el gestor de BT si salimos de la app
    override fun onDestroy() {
        super.onDestroy()
        bluetoothViewModel.stopAll()
    }
}