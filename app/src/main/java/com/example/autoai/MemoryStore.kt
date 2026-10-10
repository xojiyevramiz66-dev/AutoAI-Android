package com.example.autoai

import android.content.Context
import org.json.JSONObject
import java.io.File

class MemoryStore {
    private val file: File? = null
    private val data = mutableListOf<Pair<String, String>>()

    @Synchronized
    fun add(key: String, value: String) {
        data.add(key to value)
        if (data.size > 200) data.removeAt(0)
    }

    @Synchronized
    fun get(key: String): String? =
        data.lastOrNull { it.first == key }?.second
}
