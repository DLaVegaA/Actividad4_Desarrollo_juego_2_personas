package com.equipoea.Tankwar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creamos el DataStore (solo habrá una instancia en toda la app)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    // Esta es la "llave" para nuestro ajuste (true/false)
    private val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")

    // Un "Flow" que emite el valor actual (true/false) cada vez que cambia
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // Si no hay nada guardado, por defecto será 'false' (tema claro)
            preferences[IS_DARK_MODE_KEY] ?: false
        }

    // Función para guardar el nuevo ajuste
    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_DARK_MODE_KEY] = isDark
        }
    }
}