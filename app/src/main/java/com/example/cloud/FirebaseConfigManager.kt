package com.example.cloud

import android.content.Context
import com.example.model.FirebaseConfig
import org.json.JSONObject

class FirebaseConfigManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadActiveConfig(): FirebaseConfig {
        val savedJson = prefs.getString(KEY_OVERRIDE_CONFIG, null)
        if (!savedJson.isNullOrBlank()) {
            val parsed = parseFromJson(savedJson)
            if (parsed.isConfigured) return parsed
        }
        return loadFromAssets()
    }

    fun loadFromAssets(): FirebaseConfig {
        return try {
            val raw = context.assets.open("firebase.json").bufferedReader().use { it.readText() }
            parseFromJson(raw)
        } catch (_: Exception) {
            FirebaseConfig()
        }
    }

    fun saveActiveConfig(config: FirebaseConfig) {
        val json = toJson(config)
        prefs.edit().putString(KEY_OVERRIDE_CONFIG, json).apply()
    }

    fun hasSavedOverride(): Boolean {
        return !prefs.getString(KEY_OVERRIDE_CONFIG, null).isNullOrBlank()
    }

    fun parseFromJson(rawJson: String): FirebaseConfig {
        return try {
            val obj = JSONObject(rawJson.trim())
            val projectId = obj.optString("projectId", "").trim()
            val apiKey = obj.optString("apiKey", "").trim()
            val databaseUrl = obj.optString("databaseUrl", "")
                .ifBlank { obj.optString("databaseURL", "") }
                .trim()
                .trimEnd('/')
            val appId = obj.optString("appId", "").trim()
            val storageBucket = obj.optString("storageBucket", "").trim()

            // Support standard google-services.json structure if pasted by admin
            if (projectId.isBlank() && obj.has("project_info")) {
                val info = obj.getJSONObject("project_info")
                val gsProjectId = info.optString("project_id", "").trim()
                val gsDbUrl = info.optString("firebase_url", "").trim().trimEnd('/')
                val gsBucket = info.optString("storage_bucket", "").trim()
                var gsApiKey = ""
                var gsAppId = ""
                val clients = obj.optJSONArray("client")
                if (clients != null && clients.length() > 0) {
                    val c0 = clients.getJSONObject(0)
                    gsAppId = c0.optJSONObject("client_info")?.optString("mobilesdk_app_id", "")?.trim().orEmpty()
                    val apiKeys = c0.optJSONArray("api_key")
                    if (apiKeys != null && apiKeys.length() > 0) {
                        gsApiKey = apiKeys.getJSONObject(0).optString("current_key", "").trim()
                    }
                }
                return FirebaseConfig(
                    projectId = gsProjectId,
                    apiKey = gsApiKey,
                    databaseUrl = gsDbUrl,
                    appId = gsAppId,
                    storageBucket = gsBucket
                )
            }

            FirebaseConfig(
                projectId = projectId,
                apiKey = apiKey,
                databaseUrl = databaseUrl,
                appId = appId,
                storageBucket = storageBucket
            )
        } catch (_: Exception) {
            FirebaseConfig()
        }
    }

    fun toJson(config: FirebaseConfig): String {
        return JSONObject().apply {
            put("projectId", config.projectId.trim())
            put("apiKey", config.apiKey.trim())
            put("databaseUrl", config.databaseUrl.trim().trimEnd('/'))
            put("appId", config.appId.trim())
            put("storageBucket", config.storageBucket.trim())
        }.toString(2)
    }

    companion object {
        private const val PREFS_NAME = "lavelle_firebase_prefs"
        private const val KEY_OVERRIDE_CONFIG = "override_firebase_json"
    }
}
