package com.example.autoai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LlmClient {
    // Настрой в коде или вынеси в безопасное хранилище перед релизом.
    private val endpoint = "https://YOUR-ENDPOINT/v1/chat/completions"
    private val apiKey = "YOUR_API_KEY"
    private val model = "YOUR_MODEL"

    private val client = OkHttpClient()

    suspend fun decide(prompt: String): String {
        if (endpoint.contains("YOUR-ENDPOINT")) {
            throw IllegalStateException(
                "Настрой endpoint, API key и model в LlmClient.kt"
            )
        }

        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", "Отвечай только JSON."))
            .put(JSONObject().put("role", "user").put("content", prompt))

        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.1)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("LLM HTTP ${response.code}: $text")
            val json = JSONObject(text)
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
