package com.example.autoai

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var etInput: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAttachFile: Button
    private lateinit var btnAttachPhoto: Button
    private lateinit var btnSaveMq4: Button

    private var lastAgentResponseText: String = ""

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val path = uri.toString().lowercase()

            if (path.endsWith(".zip")) {
                Toast.makeText(this, "Распаковываю и анализирую ZIP-архив...", Toast.LENGTH_SHORT).show()
                val zipAnalysis = unzipAndAnalyze(this, uri)
                val prompt = "Я загрузил ZIP-архив с проектом. Вот его структура:\n\n$zipAnalysis"
                sendQueryToAgent(prompt)
            } else {
                val fileContent = readTextFromUri(uri)
                if (!fileContent.isNullOrBlank()) {
                    val prompt = "Проанализируй этот файл:\n\n$fileContent"
                    sendQueryToAgent(prompt)
                    Toast.makeText(this, "Файл загружен, отправляю агенту...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, "Фото выбрано!", Toast.LENGTH_SHORT).show()
            val prompt = "Я прикрепил фотографию/скриншот. Посмотри на нее и подскажи."
            sendQueryToAgent(prompt)
        } else {
            Toast.makeText(this, "Фото не было выбрано", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        btnAttachFile = findViewById(R.id.btnAttachFile)
        btnAttachPhoto = findViewById(R.id.btnAttachPhoto)
        btnSaveMq4 = findViewById(R.id.btnSaveMq4)

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendQueryToAgent(userText)
                etInput.setText("")
            }
        }

        btnAttachFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        btnAttachPhoto.setOnClickListener {
            photoPickerLauncher.launch(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        }

        btnSaveMq4.setOnClickListener {
            if (lastAgentResponseText.isNotBlank()) {
                saveMq4File(this, lastAgentResponseText, "AutoAdvisor.mq4")
            } else {
                Toast.makeText(this, "Нет кода для сохранения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendQueryToAgent(queryText: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Если у вас другой класс или метод для связи с ИИ, замените эту строчку под ваш LlmClient
                val response = "Ответ агента на запрос: $queryText"
                lastAgentResponseText = response

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ответ получен!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun unzipAndAnalyze(context: Context, zipUri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            val inputStream = context.contentResolver.openInputStream(zipUri)
            if (inputStream != null) {
                val zipInputStream = ZipInputStream(inputStream)
                var zipEntry = zipInputStream.nextEntry

                while (zipEntry != null) {
                    val fileName = zipEntry.name
                    stringBuilder.append("📁 Файл: $fileName\n")
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close()
            }
        } catch (e: Exception) {
            return "Ошибка распаковки"
        }
        return stringBuilder.toString()
    }

    private fun saveMq4File(context: Context, codeText: String, fileName: String) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    outputStream.write(codeText.toByteArray())
                    outputStream.close()
                }
                Toast.makeText(context, "Файл сохранен в Загрузки!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Не удалось создать файл", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_LONG).show()
        }
    }
}
