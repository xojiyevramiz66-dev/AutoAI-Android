package com.example.autoai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LlmClient {
    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"
    private val apiKey = "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" // Укажите ваш ключ
    private val model = "llama-3.3-7b-versatile"

    private val client = OkHttpClient()

    suspend fun decide(prompt: String): String = withContext(Dispatchers.IO) {
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
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
