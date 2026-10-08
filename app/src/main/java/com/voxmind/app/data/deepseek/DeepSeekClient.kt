package com.voxmind.app.data.deepseek

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.voxmind.app.data.models.ChatMessage
import com.voxmind.app.data.models.ExtractedReminderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class DeepSeekClient(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { "deepseek-chat" }
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val endpoint = "https://api.deepseek.com/v1/chat/completions"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = executeChatCompletion(
                systemPrompt = "You are VoxMind assistant.",
                userPrompt = "Respond with 'Connection successful!'",
                model = modelProvider()
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun organizeThoughts(transcript: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI, an expert cognitive organizer and thought structurer. 
                The user has spoken a freeflow stream of consciousness transcription.
                Structure this raw transcript into clear, elegant, actionable markdown:
                ### 💡 Core Theme & Intent
                ### 🧠 Structured Thoughts & Insights (group related points)
                ### 🎯 Key Decisions & Action Items
                Keep the tone sharp, professional, and easy to skim.
            """.trimIndent()
            val result = executeChatCompletion(system, transcript, modelProvider())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateBulletPoints(transcript: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI. Extract an actionable, concise bullet point list from the transcript.
                Output ONLY bullet points starting with '- '. 
                Do NOT include introductory phrases, markdown headers, or explanations.
                Each line must be a single actionable task or key point.
            """.trimIndent()
            val raw = executeChatCompletion(system, transcript, modelProvider())
            val bullets = raw.lines()
                .map { it.trim() }
                .filter { it.startsWith("-") || it.startsWith("*") || it.startsWith("•") }
                .map { it.removePrefix("-").removePrefix("*").removePrefix("•").trim() }
                .filter { it.isNotBlank() }

            if (bullets.isEmpty()) {
                // fallback split by lines
                val fallback = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
                Result.success(fallback)
            } else {
                Result.success(bullets)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarize(transcript: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI. Provide a high-impact executive summary of the spoken transcript:
                **TL;DR**: 1-2 sentence core message.
                
                **Key Takeaways**:
                - Key point 1
                - Key point 2
                - Key point 3
                
                **Context & Implications**: Brief summary of tone, timeline, and stakes.
            """.trimIndent()
            val result = executeChatCompletion(system, transcript, modelProvider())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractReminders(transcript: String): Result<List<ExtractedReminderItem>> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI. Analyze the spoken transcript and detect all reminders, deadlines, alarms, timers, or scheduled tasks mentioned.
                Format your response strictly as a JSON array of objects with keys:
                "taskTitle": string (e.g. "Doctor Appointment")
                "detectedDateOrTime": string (e.g. "Tomorrow at 2:00 PM" or "in 20 minutes")
                "notes": string (additional context or details)
                
                Return ONLY the JSON array without markdown backticks or commentary. Example:
                [{"taskTitle": "Call Sarah", "detectedDateOrTime": "Today at 5 PM", "notes": "Discuss project launch"}]
                If none found, return [].
            """.trimIndent()
            val raw = executeChatCompletion(system, transcript, modelProvider())
            val cleanJson = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JsonParser.parseString(cleanJson).asJsonArray
            val items = mutableListOf<ExtractedReminderItem>()
            for (elem in jsonArray) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val title = obj.get("taskTitle")?.asString ?: "Reminder"
                    val time = obj.get("detectedDateOrTime")?.asString ?: "Later"
                    val notes = obj.get("notes")?.asString ?: ""
                    items.add(ExtractedReminderItem(taskTitle = title, detectedDateOrTime = time, notes = notes))
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                addProperty("model", modelProvider())
                val msgsArray = JsonArray()
                msgsArray.add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", "You are VoxMind AI, an intelligent personal assistant powered by DeepSeek. You help organize thoughts, schedule alarms/reminders, manage task lists, and refine voice transcriptions.")
                })
                for (m in messages) {
                    msgsArray.add(JsonObject().apply {
                        addProperty("role", m.role)
                        addProperty("content", m.content)
                    })
                }
                add("messages", msgsArray)
                addProperty("temperature", 0.7)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw IOException("DeepSeek API error ${response.code}: $errBody")
                }
                val respBody = response.body?.string() ?: throw IOException("Empty response from DeepSeek")
                val jsonResp = JsonParser.parseString(respBody).asJsonObject
                val choices = jsonResp.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    val msg = choices[0].asJsonObject.getAsJsonObject("message")
                    val content = msg.get("content")?.asString ?: ""
                    Result.success(content)
                } else {
                    Result.failure(IOException("No choices returned from DeepSeek"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeChatCompletion(systemPrompt: String, userPrompt: String, model: String): String {
        val payload = JsonObject().apply {
            addProperty("model", model)
            val msgsArray = JsonArray()
            msgsArray.add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
            msgsArray.add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", userPrompt)
            })
            add("messages", msgsArray)
            addProperty("temperature", 0.5)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw IOException("DeepSeek API error ${response.code}: $errBody")
            }
            val respBody = response.body?.string() ?: throw IOException("Empty response")
            val jsonResp = JsonParser.parseString(respBody).asJsonObject
            val choices = jsonResp.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val msg = choices[0].asJsonObject.getAsJsonObject("message")
                return msg.get("content")?.asString ?: ""
            } else {
                throw IOException("No content returned in DeepSeek choice")
            }
        }
    }
}
