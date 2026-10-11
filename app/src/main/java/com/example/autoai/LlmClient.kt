package com.example.autoai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object LlmClient {
    private val client = OkHttpClient()
    private const val API_URL = "https://api.openai.com/v1/chat/completions" // Или ваш прокси/эндпоинт
    private const val API_KEY = "YOUR_API_KEY" // Замените на ваш ключ или настройте под себя

    fun sendMessage(prompt: String): String? {
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", org.json.JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Ошибка сервера: ${response.code}"
                val responseBody = response.body?.string() ?: return "Пустой ответ"
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                val message = choices.getJSONObject(0).getJSONObject("message")
                message.getString("content")
            }
        } catch (e: Exception) {
            "Ошибка запроса: ${e.localizedMessage}"
        }
    }
}
