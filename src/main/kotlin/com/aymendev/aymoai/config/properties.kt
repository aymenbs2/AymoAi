package com.aymendev.aymoai.config

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Properties

object Config {
    private val properties = Properties()

    init {
        val configFile = "config.properties"
        val inputStream: InputStream? = javaClass.classLoader.getResourceAsStream(configFile)
            ?: throw FileNotFoundException("config.properties not found in classpath")
        properties.load(inputStream)
        inputStream?.close()

    }

    val aymoApiKey: String
        get() = properties.getProperty("AYMOAPI_KEY")

    val baseUrl: String
        get() = properties.getProperty("BASE_URL")

    val helpfulAssistant: String
        get() = properties.getProperty("HELPFUL_ASSISTANT")

    val refactorCodeRq: String
        get() = properties.getProperty("REFACTOR_CODE_RQ")

    val model: String
        get() = properties.getProperty("MODEL")
    val scanRole: String
        get() = properties.getProperty("SCAN_ROLE")

    val scanRequest: String
        get() = properties.getProperty("SCAN_RQ")

    val generateUnitTestRq: String
        get() = properties.getProperty("GENERATE_UNIT_TEST_RQ")

    val refactorSelectedCodeRq: String
        get() = properties.getProperty("REFACTOR_SELECTED_CODE_RQ")
}
