package com.example.autoai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object LlmClient {
    private val client = OkHttpClient()
    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    
    // Вставьте ваш реальный Groq API ключ
    private const val API_KEY = "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"

    fun sendMessage(prompt: String): String? {
        val json = JSONObject().apply {
            // Указываем нужную модель
            put("model", "openai/gpt-oss-120b")
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
            put("temperature", 0.7)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: return "Пустой ответ от сервера"
                if (!response.isSuccessful) {
                    return "Ошибка Groq (${response.code}): $responseBody"
                }
                
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                val message = choices.getJSONObject(0).getJSONObject("message")
                message.getString("content")
            }
        } catch (e: Exception) {
            "Ошибка запроса к Groq: ${e.localizedMessage}"
        }
    }
}
