package com.equipoea.Tankwar.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.equipoea.Tankwar.model.GameMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ConnectionStatus {
    DISCONNECTED,
    LISTENING,
    SCANNING,
    CONNECTING,
    CONNECTED
}

@SuppressLint("MissingPermission")
object BluetoothConnectionManager {

    private const val TAG = "BluetoothManager"
    private const val SERVICE_NAME = "TankWar"
    private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<GameMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private var connectedSocket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private val gson = Gson()

    fun getAdapter(context: Context): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter
    }

    fun startHost(adapter: BluetoothAdapter) {
        if (status.value != ConnectionStatus.DISCONNECTED) return
        try {
            Log.d(TAG, "Iniciando Host...")
            acceptThread = AcceptThread(adapter)
            acceptThread?.start()
            _status.value = ConnectionStatus.LISTENING
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al iniciar Host", e)
            _status.value = ConnectionStatus.DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Error general al iniciar Host", e)
            _status.value = ConnectionStatus.DISCONNECTED
        }
    }

    fun startClient(adapter: BluetoothAdapter, device: BluetoothDevice) {
        if (status.value != ConnectionStatus.DISCONNECTED) return
        try {
            Log.d(TAG, "Iniciando Cliente para ${device.name}...")
            _status.value = ConnectionStatus.CONNECTING
            ConnectThread(adapter, device).start()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al iniciar Cliente", e)
            _status.value = ConnectionStatus.DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Error general al iniciar Cliente", e)
            _status.value = ConnectionStatus.DISCONNECTED
        }
    }

    fun stop() {
        Log.d(TAG, "Deteniendo todo...")
        try {
            acceptThread?.cancel()
            acceptThread = null
            connectedThread?.cancel()
            connectedThread = null
            connectedSocket?.close()
            connectedSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sockets", e)
        }
        _status.value = ConnectionStatus.DISCONNECTED
    }

    suspend fun sendMessage(message: GameMessage) {
        if (status.value != ConnectionStatus.CONNECTED) return
        try {
            // Serializamos el objeto simple
            val jsonMessage = gson.toJson(message)
            connectedThread?.write(jsonMessage.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar mensaje", e)
            stop()
        }
    }

    private fun onConnectionSuccess(socket: BluetoothSocket) {
        Log.d(TAG, "¡Conexión exitosa!")
        connectedSocket = socket
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
        _status.value = ConnectionStatus.CONNECTED
        acceptThread?.cancel()
        acceptThread = null
    }

    private fun onConnectionFailed() {
        Log.e(TAG, "Conexión fallida.")
        stop()
    }

    private class AcceptThread(adapter: BluetoothAdapter) : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Host: Socket listen() falló", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Host: Sin permisos para escuchar", e)
            }
        }

        override fun run() {
            var socket: BluetoothSocket?
            while (true) {
                try {
                    if (serverSocket == null) break
                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Host: Socket accept() falló", e)
                    break
                } catch (e: SecurityException) {
                    Log.e(TAG, "Host: Sin permisos para aceptar", e)
                    break
                }

                if (socket != null) {
                    onConnectionSuccess(socket)
                    try {
                        serverSocket?.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "Error al cerrar server socket", e)
                    }
                    break
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Host: No se pudo cerrar el server socket", e)
            }
        }
    }

    private class ConnectThread(val adapter: BluetoothAdapter, val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket?

        init {
            var tmpSocket: BluetoothSocket? = null
            try {
                tmpSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Cliente: Socket create() falló", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Cliente: Sin permisos para crear socket", e)
            }
            socket = tmpSocket
        }

        override fun run() {
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "No se pudo cancelar el descubrimiento por permisos")
            }

            try {
                if (socket == null) {
                    onConnectionFailed()
                    return
                }
                socket.connect()
            } catch (e: IOException) {
                Log.e(TAG, "Cliente: No se pudo conectar", e)
                try {
                    socket?.close()
                } catch (eClose: IOException) {
                    Log.e(TAG, "Cliente: Error al cerrar socket tras fallo", eClose)
                }
                onConnectionFailed()
                return
            } catch (e: SecurityException) {
                Log.e(TAG, "Cliente: Sin permisos para conectar", e)
                onConnectionFailed()
                return
            }

            onConnectionSuccess(socket)
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Cliente: No se pudo cerrar el socket", e)
            }
        }
    }

    private class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream

        override fun run() {
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            while (true) {
                try {
                    // Lee una línea completa (terminada en \n)
                    val messageJson = reader.readLine()
                    if (messageJson != null) {
                        Log.d(TAG, "Mensaje JSON Recibido: $messageJson")

                        // Parseo simplificado: Directo a GameMessage
                        val message = try {
                            gson.fromJson(messageJson, GameMessage::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando JSON", e)
                            null
                        }

                        if (message != null) {
                            kotlinx.coroutines.runBlocking {
                                withContext(Dispatchers.Main) {
                                    _incomingMessages.emit(message)
                                }
                            }
                        }
                    } else {
                        throw IOException("Stream cerrado por el otro extremo")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Desconexión detectada", e)
                    onConnectionFailed()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error en bucle de lectura", e)
                    break
                }
            }
        }

        suspend fun write(bytes: ByteArray) {
            withContext(Dispatchers.IO) {
                try {
                    outputStream.write(bytes)
                    // Añadimos salto de línea explícito para que readLine() funcione al otro lado
                    outputStream.write("\n".toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "Error al escribir mensaje", e)
                    onConnectionFailed()
                }
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error al cerrar socket conectado", e)
            }
        }
    }
}