package com.romnix.app.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.romnix.app.ui.screens.BacklogItem
import com.romnix.app.ui.screens.TimeSession
import kotlinx.coroutines.flow.first

// Extension property to create DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val DOWNLOAD_LOCATION_KEY = stringPreferencesKey("download_location")
        val USE_CUSTOM_LOCATION_KEY = stringPreferencesKey("use_custom_location")
        val BACKLOG_ITEMS_KEY = stringPreferencesKey("backlog_items")
    }
    
    // Flow to observe download location changes
    val downloadLocationFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DOWNLOAD_LOCATION_KEY]
        }
    
    // Flow to observe if custom location is enabled
    val useCustomLocationFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_CUSTOM_LOCATION_KEY]?.toBoolean() ?: false
        }
    
    // Save download location
    suspend fun saveDownloadLocation(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_LOCATION_KEY] = uri
        }
    }
    
    // Save custom location preference
    suspend fun setUseCustomLocation(useCustom: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_CUSTOM_LOCATION_KEY] = useCustom.toString()
        }
    }
    
    // Clear download location
    suspend fun clearDownloadLocation() {
        context.dataStore.edit { preferences ->
            preferences.remove(DOWNLOAD_LOCATION_KEY)
            preferences.remove(USE_CUSTOM_LOCATION_KEY)
        }
    }
    
    // Backlog items functions
    val backlogItemsFlow: Flow<List<BacklogItem>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[BACKLOG_ITEMS_KEY] ?: "[]"
            parseBacklogItems(json)
        }
    
    suspend fun saveBacklogItems(items: List<BacklogItem>) {
        // Use both DataStore and SharedPreferences for redundancy
        context.dataStore.edit { preferences ->
            preferences[BACKLOG_ITEMS_KEY] = backlogItemsToJson(items)
        }
        
        // Also save to SharedPreferences as backup
        val sharedPrefs = context.getSharedPreferences("backlog_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("backlog_items", backlogItemsToJson(items))
            commit() // Use commit() for immediate save
        }
    }
    
    suspend fun getBacklogItems(): List<BacklogItem> {
        // Try DataStore first
        val dataStoreItems = try {
            context.dataStore.data.first().let { preferences ->
                val json = preferences[BACKLOG_ITEMS_KEY] ?: ""
                android.util.Log.d("BacklogPersistence", "DataStore JSON: $json")
                if (json.isNotEmpty()) parseBacklogItems(json) else emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("BacklogPersistence", "DataStore read failed", e)
            emptyList()
        }
        
        // If DataStore is empty, try SharedPreferences
        if (dataStoreItems.isEmpty()) {
            val sharedPrefs = context.getSharedPreferences("backlog_prefs", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("backlog_items", "[]") ?: "[]"
            android.util.Log.d("BacklogPersistence", "Loading from SharedPrefs: $json")
            val items = parseBacklogItems(json)
            android.util.Log.d("BacklogPersistence", "Parsed ${items.size} items from SharedPrefs")
            return items
        }
        
        android.util.Log.d("BacklogPersistence", "Returning ${dataStoreItems.size} items from DataStore")
        return dataStoreItems
    }
    
    private fun parseBacklogItems(json: String): List<BacklogItem> {
        if (json == "[]" || json.isEmpty()) return emptyList()
        
        return try {
            val items = mutableListOf<BacklogItem>()
            val cleanJson = json.trim().removePrefix("[").removeSuffix("]")
            if (cleanJson.isNotEmpty()) {
                val itemObjects = cleanJson.split("},{").map { 
                    if (!it.startsWith("{")) "{$it" else it
                }.map { 
                    if (!it.endsWith("}")) "$it}" else it
                }
                
                for (obj in itemObjects) {
                    val item = parseBacklogItem(obj)
                    if (item != null) items.add(item)
                }
            }
            items
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parseBacklogItem(obj: String): BacklogItem? {
        return try {
            // Parse the main item fields
            val idMatch = """"id"\s*:\s*"([^"]*)"""".toRegex().find(obj)
            val titleMatch = """"title"\s*:\s*"([^"]*)"""".toRegex().find(obj)
            
            val id = idMatch?.groupValues?.get(1) ?: ""
            val title = titleMatch?.groupValues?.get(1) ?: ""
            
            // Parse sessions array
            val sessionsMatch = """"sessions"\s*:\s*\[([^\]]*)\]""".toRegex().find(obj)
            val sessions = if (sessionsMatch != null) {
                val sessionsContent = sessionsMatch.groupValues[1]
                if (sessionsContent.isNotBlank()) {
                    parseSessionsArray(sessionsContent)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            // Parse current session
            val currentSessionMatch = """"currentSession"\s*:\s*(\{[^}]*\}|null)""".toRegex().find(obj)
            val currentSession = if (currentSessionMatch != null && currentSessionMatch.groupValues[1] != "null") {
                parseSession(currentSessionMatch.groupValues[1])
            } else {
                null
            }
            
            BacklogItem(
                id = id,
                title = title,
                sessions = sessions,
                currentSession = currentSession
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseSessionsArray(content: String): List<TimeSession> {
        return try {
            val sessions = mutableListOf<TimeSession>()
            val sessionPattern = """\{[^}]*\}""".toRegex()
            sessionPattern.findAll(content).forEach { match ->
                parseSession(match.value)?.let { sessions.add(it) }
            }
            sessions
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseSession(obj: String): TimeSession? {
        return try {
            val idMatch = """"id"\s*:\s*"([^"]*)"""".toRegex().find(obj)
            val startMatch = """"startTime"\s*:\s*(\d+)""".toRegex().find(obj)
            val endMatch = """"endTime"\s*:\s*(\d+|null)""".toRegex().find(obj)
            
            val id = idMatch?.groupValues?.get(1) ?: java.util.UUID.randomUUID().toString()
            val startTime = startMatch?.groupValues?.get(1)?.toLongOrNull() ?: return null
            val endTime = endMatch?.groupValues?.get(1)?.let { 
                if (it != "null") it.toLongOrNull() else null
            }
            
            TimeSession(
                id = id,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun backlogItemsToJson(items: List<BacklogItem>): String {
        if (items.isEmpty()) return "[]"
        
        val itemStrings = items.map { item ->
            val sessionsJson = item.sessions.joinToString(",") { session ->
                "{\"id\":\"${session.id}\",\"startTime\":${session.startTime},\"endTime\":${session.endTime ?: "null"}}"
            }
            
            val currentSessionJson = item.currentSession?.let { session ->
                "{\"id\":\"${session.id}\",\"startTime\":${session.startTime},\"endTime\":${session.endTime ?: "null"}}"
            } ?: "null"
            
            "{\"id\":\"${item.id}\",\"title\":\"${item.title}\",\"sessions\":[${sessionsJson}],\"currentSession\":${currentSessionJson}}"
        }
        return "[${itemStrings.joinToString(",")}]"
    }
}

