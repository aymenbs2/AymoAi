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
import com.aymendev.aymoai.config.Config
import com.aymendev.aymoai.util.RequestHelper
import java.util.concurrent.TimeUnit


class MainAiServices(private val apiKey: String) {
    private var client = OkHttpClient.Builder()
        .connectTimeout(5000L, TimeUnit.SECONDS) // Set the connection timeout
        .writeTimeout(5000L, TimeUnit.SECONDS)   // Set the write timeout
        .readTimeout(10000L, TimeUnit.SECONDS)    // Set the read timeout
        .build()




    private val gson = Gson()

    fun generateUnitTest(codeContent: String, callback: (Result<String>) -> Unit) {
        client=  RequestHelper.createHttpClientBasedOnFileSize(codeContent.length.toLong())
        val requestBodyMap = mapOf(
            "model" to Config.model,
            "messages" to listOf(
                mapOf(
                    "role" to SYSTEM_ROLE,
                    "content" to Config.helpfulAssistant
                ),
                mapOf(
                    "role" to USER_ROLE,
                    "content" to "${Config.generateUnitTestRq}\n $codeContent"
                )
            )
        )

        val requestBodyJson = gson.toJson(requestBodyMap)
        val request = Request.Builder()
            .url(Config.baseUrl)
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

    fun refactCode(rq:String,codeContent: String, callback: (Result<String>) -> Unit) {
        client=  RequestHelper.createHttpClientBasedOnFileSize(codeContent.length.toLong())

        val requestBodyMap = mapOf(
            "model" to Config.model,
            "messages" to listOf(
                mapOf(
                    "role" to SYSTEM_ROLE,
                    "content" to Config.helpfulAssistant
                ),
                mapOf(
                    "role" to USER_ROLE,
                    "content" to "${rq}\n $codeContent"
                )
            )
        )

        val requestBodyJson = gson.toJson(requestBodyMap)
        val request = Request.Builder()
            .url(Config.baseUrl)
            .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("Content-Type", CONTENT_TYPE)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("err:${e.message}")

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
