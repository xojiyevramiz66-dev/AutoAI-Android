package com.example.autoai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LlmClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val API_KEY = "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"

    // История сообщений: хранит системный промпт и диалог
    private val conversationHistory = JSONArray().apply {
        put(JSONObject()
            .put("role", "system")
            .put("content", "Ты полезный и вежливый AI-ассистент для разработчика. Всегда отвечай строго на русском языке."))
    }

    fun sendMessage(prompt: String): String? {
        // Добавляем новое сообщение пользователя
        conversationHistory.put(JSONObject().put("role", "user").put("content", prompt))

        // Ограничиваем размер истории (оставляем системный промпт [индекс 0] + последние 10 сообщений),
        // чтобы избежать ошибки 413 (Request too large)
        if (conversationHistory.length() > 11) {
            val systemMessage = conversationHistory.getJSONObject(0)
            val recentMessages = JSONArray()
            recentMessages.put(systemMessage)
            
            // Берем последние 10 элементов из старого массива
            val startIdx = conversationHistory.length() - 10
            for (i in startIdx until conversationHistory.length()) {
                recentMessages.put(conversationHistory.getJSONObject(i))
            }
            
            // Перезаписываем историю
            conversationHistory.let {
                for (i in it.length() - 1 downTo 0) {
                    it.remove(i)
                }
                for (i in 0 until recentMessages.length()) {
                    it.put(recentMessages.getJSONObject(i))
                }
            }
        }

        val json = JSONObject().apply {
            put("model", "openai/gpt-oss-120b")
            put("messages", conversationHistory)
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
                val messageObj = choices.getJSONObject(0).getJSONObject("message")
                val answerText = messageObj.getString("content")

                // Сохраняем ответ ассистента в историю, чтобы бот помнил контекст
                conversationHistory.put(JSONObject().put("role", "assistant").put("content", answerText))

                answerText
            }
        } catch (e: Exception) {
            "Ошибка запроса к Groq: ${e.localizedMessage}"
        }
    }
}
