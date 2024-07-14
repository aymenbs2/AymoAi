package com.aymendev.aymoai.network

import com.aymendev.aymoai.util.Constants.CONTENT_TYPE
import com.aymendev.aymoai.util.Constants.ERROR_EMPTY_RESPONSE_BODY

import com.aymendev.aymoai.util.Constants.SYSTEM_ROLE
import com.aymendev.aymoai.util.Constants.USER_ROLE
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainAiServices(private val apiKey: String) {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun generateUnitTest(codeContent: String, callback: (Result<String>) -> Unit) {
        val requestBodyMap = mapOf(
            "model" to System.getenv("MODEL"),
            "messages" to listOf(
                mapOf(
                    "role" to SYSTEM_ROLE,
                    "content" to System.getenv("HELPFUL_ASSISTANT")
                ),
                mapOf(
                    "role" to USER_ROLE,
                    "content" to "${System.getenv("GENERATE_UNIT_TEST_RQ")}\n $codeContent"
                )
            )
        )

        val requestBodyJson = gson.toJson(requestBodyMap)
        val request = Request.Builder()
            .url(System.getenv("BASE_URL"))
            .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("Content-Type", CONTENT_TYPE)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val jsonResponse = JsonParser.parseString(it).asJsonObject
                    val choices = jsonResponse.getAsJsonArray("choices")
                    val unitTestContent = choices[0].asJsonObject
                        .getAsJsonObject("message")
                        .get("content").asString
                    callback(Result.success(unitTestContent))
                } ?:
                callback(Result.failure(IOException(ERROR_EMPTY_RESPONSE_BODY)))
            }
        })
    }

    fun refactCode(codeContent: String, callback: (Result<String>) -> Unit) {
        val requestBodyMap = mapOf(
            "model" to System.getenv("MODEL"),
            "messages" to listOf(
                mapOf(
                    "role" to SYSTEM_ROLE,
                    "content" to System.getenv("HELPFUL_ASSISTANT")
                ),
                mapOf(
                    "role" to USER_ROLE,
                    "content" to "${System.getenv("REFACTOR_CODE_RQ")}\n $codeContent"
                )
            )
        )

        val requestBodyJson = gson.toJson(requestBodyMap)
        val request = Request.Builder()
            .url(System.getenv("BASE_URL"))
            .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("Content-Type", CONTENT_TYPE)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(IOException("Server Error")))
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val jsonResponse = JsonParser.parseString(it).asJsonObject
                    val choices = jsonResponse.getAsJsonArray("choices")
                    val unitTestContent = choices[0].asJsonObject
                        .getAsJsonObject("message")
                        .get("content").asString
                    callback(Result.success(unitTestContent))
                } ?:
                callback(Result.failure(IOException(ERROR_EMPTY_RESPONSE_BODY)))
            }
        })
    }

}
