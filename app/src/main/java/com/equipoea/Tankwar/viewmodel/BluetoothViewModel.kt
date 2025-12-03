package com.equipoea.Tankwar.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.equipoea.Tankwar.data.BluetoothConnectionManager
import com.equipoea.Tankwar.data.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    val bluetoothAdapter: BluetoothAdapter? = BluetoothConnectionManager.getAdapter(context)

    // Estado de la conexión
    val connectionStatus = BluetoothConnectionManager.status

    // Lista de dispositivos emparejados
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    // Lista de dispositivos escaneados
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    // --- CORRECCIÓN: Variable para recordar si somos Host ---
    var isAmIHost: Boolean = false
        private set

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    if (device != null && device.name != null) {
                        _scannedDevices.update { list ->
                            if (list.any { it.address == device.address }) list else list + device
                        }
                    }
                }
            }
        }
    }

    init {
        // Eliminamos updatePairedDevices() de aquí para evitar crash de permisos
    }

    fun updatePairedDevices() {
        if (bluetoothAdapter?.isEnabled == true) {
            try {
                _pairedDevices.value = bluetoothAdapter.bondedDevices.toList()
            } catch (e: SecurityException) {
                Log.e("BluetoothViewModel", "Sin permiso para ver dispositivos emparejados", e)
                _pairedDevices.value = emptyList()
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error al obtener dispositivos emparejados", e)
            }
        }
    }

    fun startHost() {
        // --- CORRECCIÓN: Recordar que somos Host ---
        isAmIHost = true
        if (bluetoothAdapter?.isEnabled == true) {
            BluetoothConnectionManager.startHost(bluetoothAdapter)
        }
    }

    fun startScan() {
        if (bluetoothAdapter?.isEnabled == true) {
            try {
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                context.registerReceiver(scanReceiver, filter)
                bluetoothAdapter.startDiscovery()
            } catch (e: SecurityException) {
                Log.e("BluetoothViewModel", "Sin permiso para escanear", e)
            }
        }
    }

    fun startClient(device: BluetoothDevice) {
        // --- CORRECCIÓN: Recordar que NO somos Host (somos Cliente) ---
        isAmIHost = false
        if (bluetoothAdapter?.isEnabled == true) {
            BluetoothConnectionManager.startClient(bluetoothAdapter, device)
        }
    }

    fun stopAll() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            // Ignorar
        }

        try {
            context.unregisterReceiver(scanReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignorar
        }
        BluetoothConnectionManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}