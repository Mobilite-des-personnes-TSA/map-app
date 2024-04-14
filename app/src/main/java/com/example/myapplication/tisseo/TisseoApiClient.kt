package com.example.myapplication.tisseo

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class TisseoApiClient {

    private fun executeGetRequest(service: String, parameters: Map<String, String>): String? {
        val urlBuilder = StringBuilder(ENTRY_POINT)
            .append(service).append(".json?key=").append(API_KEY)
        parameters.forEach { (k, v) -> urlBuilder.append("&").append(k).append("=").append(v) }
        Log.d("TisseoApiClient", "URL: $urlBuilder")
        val connection = URL(urlBuilder.toString()).openConnection() as HttpsURLConnection
        val result = try {
            connection.requestMethod = "GET"
            connection.connect()
            BufferedInputStream(connection.inputStream).bufferedReader()
                .use { it.readText() }
        } catch (e: IOException) {
            null
        } finally {
            connection.disconnect()
        }
        Log.d("TisseoApiClient", "Result: $result")
        return result
    }

    fun apiAutocomplete(parameter: String) = executeGetRequest(
        "places", mapOf("term" to parameter)
    )

    fun apiJourney(
        departurePlace: String,
        arrivalPlace: String,
        roadMode: String,
        number: String,
        displayWording: String = "1",
        lang: String = "fr"
    ) = executeGetRequest(
        "journeys",
        mapOf(
            "departurePlace" to departurePlace,
            "arrivalPlace" to arrivalPlace,
            "roadMode" to roadMode,
            "number" to number,
            "displayWording" to displayWording,
            "lang" to lang
        )
    )

    companion object {
        private const val ENTRY_POINT = "https://api.tisseo.fr/v2/"
    }
}
