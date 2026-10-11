package com.example.autoai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LlmClient {
    // Настраиваем клиент с таймаутами для стабильной работы
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    
    // Ваш API ключ Groq[span_2](start_span)[span_2](end_span)
    private const val API_KEY = "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"

    fun sendMessage(prompt: String): String? {
        val json = JSONObject().apply {
            // Исправлено: заменена проблемная модель на быструю и стабильную llama-3.1-8b-instant, 
            // у которой нормальные лимиты и не возникает ошибка 413.
            put("model", "openai/gpt-oss-120b")
            
            // Формируем список сообщений с системным промптом
            val messages = JSONArray().apply {
                // Системная инструкция фиксирует поведение бота и язык
                put(JSONObject()
                    .put("role", "system")
                    .put("content", "Ты полезный и вежливый AI-ассистент для разработчика. Всегда отвечай строго на русском языке, кратко, понятно и по делу."))
                
                // Запрос пользователя
                put(JSONObject()
                    .put("role", "user")
                    .put("content", prompt))
            }
            
            put("messages", messages)
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
