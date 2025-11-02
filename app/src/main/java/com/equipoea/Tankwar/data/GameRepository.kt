package com.equipoea.Tankwar.data

import android.content.Context
import android.util.Log
import com.equipoea.Tankwar.model.GameState
import com.google.gson.Gson // Solo importamos Gson
import java.io.File

class GameRepository(private val context: Context) {

    private val saveDir = File(context.filesDir, "saved_games")
    private val gson = Gson()
    // --- BORRADO: Ya no necesitamos XmlMapper ---
    // private val xmlMapper = XmlMapper().registerModule(KotlinModule())

    init {
        if (!saveDir.exists()) {
            saveDir.mkdirs()
        }
    }

    /**
     * Guarda el estado del juego en un archivo JSON.
     */
    fun saveGame(gameState: GameState, fileName: String) {
        try {
            // Guardar como JSON
            val jsonString = gson.toJson(gameState)
            File(saveDir, "$fileName.json").writeText(jsonString)

            // --- BORRADO: Ya no guardamos en XML ---

            Log.d("GameRepository", "Partida guardada: $fileName.json")
        } catch (e: Exception) {
            Log.e("GameRepository", "Error al guardar partida", e)
        }
    }

    /**
     * Carga un GameState desde un archivo JSON.
     */
    fun loadGame(fileName: String): GameState? {
        return try {
            val file = File(saveDir, fileName)
            if (!file.exists()) return null

            val jsonString = file.readText()
            gson.fromJson(jsonString, GameState::class.java)
        } catch (e: Exception) {
            Log.e("GameRepository", "Error al cargar partida", e)
            null
        }
    }

    /**
     * Devuelve una lista de los nombres de archivos de las partidas guardadas.
     */
    fun getSavedGamesList(): List<String> {
        return saveDir.listFiles { _, name -> name.endsWith(".json") }
            ?.map { it.name }
            ?: emptyList()
    }
    /*
    Elimina los archivos .json y .xml de una partida guardada.
    */
    fun deleteGame(fileName: String) {
        try {
            val jsonFile = File(saveDir, fileName)
            // Asegúrate de borrar también el archivo .xml correspondiente
            val xmlFile = File(saveDir, fileName.replace(".json", ".xml"))

            var deletedJson = false
            var deletedXml = false

            if (jsonFile.exists()) {
                jsonFile.delete()
                deletedJson = true
            }
            if (xmlFile.exists()) {
                xmlFile.delete()
                deletedXml = true
            }

            Log.d("GameRepository", "Partida eliminada: $fileName (JSON: $deletedJson, XML: $deletedXml)")
        } catch (e: Exception) {
            Log.e("GameRepository", "Error al eliminar partida", e)
        }
    }
}