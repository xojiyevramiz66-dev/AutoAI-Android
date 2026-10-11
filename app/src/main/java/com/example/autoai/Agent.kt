package com.example.autoai

import org.json.JSONObject

object Agent {
    fun decide(prompt: String): String? {
        val response = LlmClient.sendMessage(prompt) ?: return "Нет ответа от LLM"
        
        return try {
            if (response.trim().startsWith("{")) {
                val json = JSONObject(response)
                json.optString("content", response)
            } else {
                response
            }
        } catch (e: Exception) {
            response
        }
    }
}
