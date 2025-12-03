package com.equipoea.Tankwar.ui.menu

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.equipoea.Tankwar.data.ConnectionStatus
import com.equipoea.Tankwar.viewmodel.BluetoothViewModel

// Lista de permisos requeridos
private val BLUETOOTH_PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE
    )
} else {
    arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}

@Composable
fun BluetoothLobbyScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()

    var hasPermissions by remember { mutableStateOf(false) }

    // 1. Launcher de Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            viewModel.updatePairedDevices()
        }
    }

    // 2. Launcher para activar Bluetooth
    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            permissionLauncher.launch(BLUETOOTH_PERMISSIONS)
        }
    }

    // 3. Launcher para hacer VISIBLE el dispositivo
    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.startHost()
    }

    LaunchedEffect(Unit) {
        if (viewModel.bluetoothAdapter == null) return@LaunchedEffect

        if (!viewModel.bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        } else {
            permissionLauncher.launch(BLUETOOTH_PERMISSIONS)
        }
    }

    // --- CORRECCIÓN CLAVE AQUÍ ---
    LaunchedEffect(connectionStatus) {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            // Usamos la variable que recuerda qué botón pulsamos
            val localPlayerId = if (viewModel.isAmIHost) 1 else 2

            navController.navigate("game_screen/PVP_BLUETOOTH/NINGUNA?restored=false&localPlayerId=$localPlayerId") {
                popUpTo("start_screen")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Lobby de Bluetooth", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (connectionStatus != ConnectionStatus.DISCONNECTED) {
            LoadingContent(connectionStatus) { viewModel.stopAll() }
        } else if (hasPermissions) {
            LobbyContent(
                pairedDevices = pairedDevices,
                scannedDevices = scannedDevices,
                onHostClick = {
                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                    discoverableLauncher.launch(discoverableIntent)
                },
                onScanClick = { viewModel.startScan() },
                onDeviceClick = { viewModel.startClient(it) }
            )
        } else {
            Text("Se requieren permisos de Bluetooth para jugar.")
            Button(onClick = { permissionLauncher.launch(BLUETOOTH_PERMISSIONS) }) {
                Text("Otorgar Permisos")
            }
        }
    }
}

@Composable
private fun LoadingContent(status: ConnectionStatus, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val statusText = when (status) {
            ConnectionStatus.LISTENING -> "Esperando conexión..."
            ConnectionStatus.SCANNING -> "Buscando dispositivos..."
            ConnectionStatus.CONNECTING -> "Conectando..."
            else -> "Cargando..."
        }
        Text(text = statusText, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
private fun LobbyContent(
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onHostClick: () -> Unit,
    onScanClick: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Button(onClick = onHostClick, modifier = Modifier.fillMaxWidth()) {
        Text("Crear Partida (Ser Host y Visible)")
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(text = "Dispositivos Emparejados", style = MaterialTheme.typography.titleMedium)
    DeviceList(devices = pairedDevices, onDeviceClick = onDeviceClick)

    Spacer(modifier = Modifier.height(16.dp))
    Divider()
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Dispositivos Cercanos", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onScanClick) { Text("Escanear") }
    }
    DeviceList(devices = scannedDevices, onDeviceClick = onDeviceClick)
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceList(devices: List<BluetoothDevice>, onDeviceClick: (BluetoothDevice) -> Unit) {
    if (devices.isEmpty()) {
        Text(text = "Ningún dispositivo encontrado.", modifier = Modifier.padding(vertical = 8.dp))
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
            items(devices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDeviceClick(device) }
                ) {
                    Text(
                        text = device.name ?: device.address,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}