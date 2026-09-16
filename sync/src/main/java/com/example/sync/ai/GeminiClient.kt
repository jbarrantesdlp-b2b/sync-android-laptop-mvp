package com.example.sync.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiContextAction(val displayName: String, val systemInstruction: String) {
    SUMMARIZE(
        "Resumir",
        "Eres el asistente IA de Sync Engine. Resume el siguiente contenido de manera clara, concisa y estructurada en viñetas clave."
    ),
    TRANSLATE(
        "Traducir",
        "Eres el asistente IA de Sync Engine. Si el siguiente texto está en español, tradúcelo a inglés profesional. Si está en otro idioma, tradúcelo a español neutro. Devuelve solo la traducción."
    ),
    CORRECT(
        "Corregir",
        "Eres el asistente IA de Sync Engine. Corrige ortografía, puntuación, sintaxis y estilo del siguiente texto, manteniendo el significado original."
    ),
    EXTRACT_ACTIONS(
        "Extraer acciones",
        "Eres el asistente IA de Sync Engine. Analiza el siguiente contenido y extrae una lista numerada de acciones concretas, tareas pendientes y fechas si existen."
    ),
    IMPROVE(
        "Mejorar",
        "Eres el asistente IA de Sync Engine. Reescribe y eleva el siguiente texto para que tenga un tono ejecutivo, tecnológico, limpio y profesional."
    )
}

class GeminiClient(
    private val apiKeyProvider: () -> String?
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun executeAction(
        action: AiContextAction,
        contextText: String,
        userQuery: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Clave GEMINI_API_KEY no configurada. Configúrala en Ajustes.")
            )
        }

        val prompt = buildString {
            append(action.systemInstruction)
            append("\n\n--- CONTENIDO CONTEXTUAL ---\n")
            append(contextText.trim())
            if (!userQuery.isNullOrBlank()) {
                append("\n\n--- INSTRUCCIÓN ADICIONAL DEL USUARIO ---\n")
                append(userQuery.trim())
            }
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        JSONObject(body).optJSONObject("error")?.optString("message") ?: "Error HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Error HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception("Gemini API: $errMsg"))
                }

                val jsonResponse = JSONObject(body)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val resultText = parts.getJSONObject(0).optString("text")
                        return@withContext Result.success(resultText.trim())
                    }
                }
                Result.failure(Exception("Respuesta vacía de Gemini API"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
