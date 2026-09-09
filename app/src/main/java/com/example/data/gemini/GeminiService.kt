package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        val KNOWN_MODELS = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-3.7-flash",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash-image"
        )
    }

    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key is missing. Please enter your Gemini API key in Settings."))
        }
        val startTime = System.currentTimeMillis()
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Respond with the single word: OK") })
                    })
                })
            })
        }
        val cleanModel = model.trim().removePrefix("models/")
        val url = "$BASE_URL/models/$cleanModel:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val latency = System.currentTimeMillis() - startTime
            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(body)
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
            Result.success("Connection successful! ($latency ms, Model: $cleanModel)")
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.success(KNOWN_MODELS)
        }
        val url = "$BASE_URL/models?key=$apiKey"
        val request = Request.Builder().url(url).get().build()
        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.success(KNOWN_MODELS)
            }
            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: return@withContext Result.success(KNOWN_MODELS)
            val modelNames = mutableListOf<String>()
            for (i in 0 until modelsArray.length()) {
                val m = modelsArray.getJSONObject(i)
                val name = m.optString("name").removePrefix("models/")
                val supportedMethods = m.optJSONArray("supportedGenerationMethods")
                var canGenerate = false
                if (supportedMethods != null) {
                    for (j in 0 until supportedMethods.length()) {
                        if (supportedMethods.getString(j) == "generateContent") {
                            canGenerate = true
                            break
                        }
                    }
                }
                if (canGenerate && !name.contains("deprecated", true) && !name.contains("embedding", true)) {
                    modelNames.add(name)
                }
            }
            if (modelNames.isEmpty()) Result.success(KNOWN_MODELS) else Result.success(modelNames)
        } catch (e: Exception) {
            Result.success(KNOWN_MODELS)
        }
    }

    suspend fun lookupWord(word: String, apiKey: String, model: String): Result<WordLookupResult> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an advanced bilingual English-Vietnamese lexicographer and tutor.
            Analyze the following word or phrase: "$word".
            Return ONLY a raw JSON object (no markdown, no code blocks) with this exact schema:
            {
              "word": "$word",
              "partOfSpeech": "noun / verb / adjective / etc.",
              "ipa": "/.../",
              "pronunciation": "plain english phonetic guide",
              "englishDefinition": "Clear, concise definition in English",
              "vietnameseTranslation": "Nghĩa tiếng Việt chuẩn xác, tự nhiên",
              "cefr": "A1/A2/B1/B2/C1/C2",
              "examples": ["English example sentence 1", "English example sentence 2"],
              "exampleTranslations": ["Bản dịch ví dụ 1", "Bản dịch ví dụ 2"],
              "synonyms": ["synonym 1", "synonym 2"],
              "antonyms": ["antonym 1", "antonym 2"],
              "collocations": ["common collocation 1", "collocation 2"],
              "wordFamily": ["related word 1", "related word 2"],
              "commonMistakes": "Brief tip on common learner mistake",
              "memoryTip": "Catchy mnemonic or association to remember this word easily"
            }
        """.trimIndent()

        val textResult = callGeminiText(prompt, apiKey, model)
        textResult.fold(
            onSuccess = { raw ->
                try {
                    val cleanJson = cleanJsonString(raw)
                    val obj = JSONObject(cleanJson)
                    val result = WordLookupResult(
                        word = obj.optString("word", word),
                        partOfSpeech = obj.optString("partOfSpeech", ""),
                        ipa = obj.optString("ipa", ""),
                        pronunciation = obj.optString("pronunciation", ""),
                        englishDefinition = obj.optString("englishDefinition", ""),
                        vietnameseTranslation = obj.optString("vietnameseTranslation", ""),
                        cefr = obj.optString("cefr", "B1"),
                        examples = jsonArrayToList(obj.optJSONArray("examples")),
                        exampleTranslations = jsonArrayToList(obj.optJSONArray("exampleTranslations")),
                        synonyms = jsonArrayToList(obj.optJSONArray("synonyms")),
                        antonyms = jsonArrayToList(obj.optJSONArray("antonyms")),
                        collocations = jsonArrayToList(obj.optJSONArray("collocations")),
                        wordFamily = jsonArrayToList(obj.optJSONArray("wordFamily")),
                        commonMistakes = obj.optString("commonMistakes", ""),
                        memoryTip = obj.optString("memoryTip", "")
                    )
                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse dictionary response: ${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun generateFlashcards(
        topicOrInput: String,
        cefr: String,
        difficulty: String,
        count: Int,
        languagePair: String,
        cardType: String,
        apiKey: String,
        model: String
    ): Result<List<GeneratedCardItem>> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate $count high-quality educational flashcards based on the following instructions:
            Topic/Source: "$topicOrInput"
            CEFR Level: $cefr
            Difficulty: $difficulty
            Card Type: $cardType
            Target Language Pair: $languagePair (Front: English, Back: Meaning & Vietnamese)

            Return ONLY a raw JSON array of objects (no markdown, no backticks), each with:
            [
              {
                "front": "Word or Phrase",
                "back": "English definition",
                "translation": "Vietnamese translation",
                "example": "Natural contextual example sentence",
                "ipa": "/phonetic ipa/",
                "pronunciation": "phonetic respelling",
                "notes": "Grammar or usage notes",
                "mnemonic": "Memory association tip",
                "tags": "$topicOrInput,$cefr,$difficulty",
                "difficulty": "$difficulty"
              }
            ]
        """.trimIndent()

        val textResult = callGeminiText(prompt, apiKey, model)
        textResult.fold(
            onSuccess = { raw ->
                try {
                    val clean = cleanJsonString(raw)
                    val array = JSONArray(clean)
                    val list = mutableListOf<GeneratedCardItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            GeneratedCardItem(
                                front = obj.optString("front", ""),
                                back = obj.optString("back", ""),
                                translation = obj.optString("translation", ""),
                                example = obj.optString("example", ""),
                                ipa = obj.optString("ipa", ""),
                                pronunciation = obj.optString("pronunciation", ""),
                                notes = obj.optString("notes", ""),
                                mnemonic = obj.optString("mnemonic", ""),
                                tags = obj.optString("tags", topicOrInput),
                                difficulty = obj.optString("difficulty", difficulty),
                                isSelected = true
                            )
                        )
                    }
                    Result.success(list)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse generated cards: ${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun extractFlashcardsFromImage(
        bitmap: Bitmap,
        instructions: String,
        apiKey: String,
        model: String
    ): Result<List<GeneratedCardItem>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required."))
        }
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val promptText = """
            Extract key English vocabulary, phrases, idioms or sentences from this image/document.
            User instructions: $instructions
            Return ONLY a raw JSON array of objects (no markdown, no backticks):
            [
              {
                "front": "English term / phrase found in the image",
                "back": "Clear definition in English",
                "translation": "Vietnamese meaning",
                "example": "Example sentence using the word",
                "ipa": "/IPA/",
                "pronunciation": "phonetic guide",
                "notes": "Context from document",
                "mnemonic": "Memory tip",
                "tags": "OCR,Extracted",
                "difficulty": "Medium"
              }
            ]
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        // Vision requires a vision-capable model
        val cleanModel = if (model.contains("image", true)) model else "gemini-2.5-flash-image"
        val url = "$BASE_URL/models/$cleanModel:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Image analysis failed: ${extractErrorMessage(body)}"))
            }
            val text = parseCandidateText(body)
            val clean = cleanJsonString(text)
            val array = JSONArray(clean)
            val list = mutableListOf<GeneratedCardItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GeneratedCardItem(
                        front = obj.optString("front", ""),
                        back = obj.optString("back", ""),
                        translation = obj.optString("translation", ""),
                        example = obj.optString("example", ""),
                        ipa = obj.optString("ipa", ""),
                        pronunciation = obj.optString("pronunciation", ""),
                        notes = obj.optString("notes", ""),
                        mnemonic = obj.optString("mnemonic", ""),
                        tags = obj.optString("tags", "OCR"),
                        difficulty = obj.optString("difficulty", "Medium"),
                        isSelected = true
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(Exception("OCR processing error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun generateQuizQuestions(
        cardsSummary: String,
        quizType: String,
        count: Int,
        apiKey: String,
        model: String
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate $count interactive quiz questions of type '$quizType' using these flashcards:
            $cardsSummary

            Quiz Types can include:
            - MCQ: 4 options (A, B, C, D)
            - TRUE_FALSE: Question statement and correctAnswer ("True" or "False")
            - FILL_BLANK: Sentence with '_____' blank, and the missing word
            - TYPING: Given definition/translation, prompt user to type target English word
            - TRANSLATION: Translate target word/sentence to Vietnamese or vice versa
            - MATCHING: Key-value matching pairs

            Return ONLY a raw JSON array of question objects (no markdown, no backticks):
            [
              {
                "id": "q1",
                "type": "$quizType",
                "question": "The question text or sentence with blank",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctAnswer": "Exact correct answer string",
                "explanation": "Brief and clear explanation of why this answer is correct",
                "matchingPairs": {"word1": "def1", "word2": "def2"}
              }
            ]
        """.trimIndent()

        val textResult = callGeminiText(prompt, apiKey, model)
        textResult.fold(
            onSuccess = { raw ->
                try {
                    val clean = cleanJsonString(raw)
                    val array = JSONArray(clean)
                    val list = mutableListOf<QuizQuestion>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pairs = mutableMapOf<String, String>()
                        val pairsObj = obj.optJSONObject("matchingPairs")
                        if (pairsObj != null) {
                            val keys = pairsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                pairs[k] = pairsObj.getString(k)
                            }
                        }
                        list.add(
                            QuizQuestion(
                                id = obj.optString("id", "q$i"),
                                type = obj.optString("type", quizType),
                                question = obj.optString("question", ""),
                                options = jsonArrayToList(obj.optJSONArray("options")),
                                correctAnswer = obj.optString("correctAnswer", ""),
                                explanation = obj.optString("explanation", ""),
                                matchingPairs = pairs
                            )
                        )
                    }
                    Result.success(list)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse quiz: ${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun askAiTutor(
        userQuery: String,
        history: List<TutorMessage>,
        apiKey: String,
        model: String
    ): Result<TutorMessage> = withContext(Dispatchers.IO) {
        val systemPrompt = "You are Qusin's Personal English Tutor. You explain grammar rules, vocabulary nuances, pronunciation, and idioms with clarity, warmth, and accuracy. Always give clear examples and Vietnamese explanations when helpful."
        val prompt = buildString {
            appendLine(systemPrompt)
            appendLine("Previous conversation:")
            for (m in history.takeLast(6)) {
                appendLine("${m.sender.uppercase()}: ${m.text}")
            }
            appendLine("USER: $userQuery")
            appendLine("Respond directly with your explanation, example sentences, and highlight any useful vocabulary.")
        }
        val textResult = callGeminiText(prompt, apiKey, model)
        textResult.map { text ->
            TutorMessage(
                sender = "tutor",
                text = text,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun chatConversationPractice(
        role: String, // Teacher, Friend, Interviewer, IELTS examiner, Tourist, Customer, Coworker
        userMessage: String,
        history: List<TutorMessage>,
        apiKey: String,
        model: String
    ): Result<TutorMessage> = withContext(Dispatchers.IO) {
        val prompt = """
            You are roleplaying as a '$role' in an interactive conversational English practice session.
            Reply naturally to the user in character as a $role.
            Then, in a structured feedback section, provide:
            1. Any grammar or vocabulary corrections for the user's message.
            2. Better native alternative ways to say it.
            3. Useful vocabulary from this exchange.

            Format your response as raw JSON ONLY:
            {
              "reply": "Your in-character dialogue as $role",
              "corrections": "Feedback on grammar/word choice, or 'Great natural expression!' if perfect",
              "suggestedVocab": ["vocab1", "vocab2"]
            }

            USER'S LAST MESSAGE: "$userMessage"
        """.trimIndent()

        val textResult = callGeminiText(prompt, apiKey, model)
        textResult.fold(
            onSuccess = { raw ->
                try {
                    val clean = cleanJsonString(raw)
                    val obj = JSONObject(clean)
                    Result.success(
                        TutorMessage(
                            sender = "tutor",
                            text = obj.optString("reply", raw),
                            corrections = obj.optString("corrections", ""),
                            suggestedVocab = jsonArrayToList(obj.optJSONArray("suggestedVocab")),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Result.success(
                        TutorMessage(
                            sender = "tutor",
                            text = raw,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun callGeminiText(prompt: String, apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required. Please add it in Settings."))
        }
        val cleanModel = model.trim().removePrefix("models/")
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
            })
        }

        val url = "$BASE_URL/models/$cleanModel:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(body)
                return@withContext Result.failure(Exception("Gemini error (${response.code}): $errorMsg"))
            }
            val text = parseCandidateText(body)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseCandidateText(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val first = candidates.getJSONObject(0)
        val content = first.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun extractErrorMessage(responseBody: String): String {
        return try {
            val root = JSONObject(responseBody)
            val error = root.optJSONObject("error")
            error?.optString("message", responseBody) ?: responseBody
        } catch (_: Exception) {
            responseBody
        }
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json").trim()
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```").trim()
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```").trim()
        }
        val firstBracket = str.indexOfAny(charArrayOf('{', '['))
        val lastBracket = Math.max(str.lastIndexOf('}'), str.lastIndexOf(']'))
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            str = str.substring(firstBracket, lastBracket + 1)
        }
        return str
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }
}
