package com.example.myapplication.tisseo

import android.net.Uri
import android.util.Log
import com.example.myapplication.BuildConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection


object TisseoApiClient {
    private val apiEntryUri =
        Uri.Builder().scheme("https").authority("api.tisseo.fr").appendPath("v2")
            .appendQueryParameter("key", BuildConfig.TISSEO_API_KEY).build()

    private fun <T> executeGetRequest(deserializer: DeserializationStrategy<T>, uri: Uri): T? {
        Log.d("TisseoApiClient", "Request: $uri")
        val connection = URL(uri.toString()).openConnection() as HttpsURLConnection
        val result = try {
            BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.wtf("TisseoApiClient", e.message, e)
            null
        } finally {
            connection.disconnect()
        }
        Log.d("TisseoApiClient", "Result: $result")
        return result?.let { Json.decodeFromString(deserializer, it) }
    }


    @ExperimentalSerializationApi
    fun places(
        term: String = "", coordinatesXY: String = "", lang: String = "fr",
    ) = executeGetRequest(
        PlacesResponse.serializer(),
        apiEntryUri.buildUpon().appendPath("places.json")
            .appendQueryParameter("term", term)
            .appendQueryParameter("coordinatesXY", coordinatesXY)
            .appendQueryParameter("lang", lang)
            .build(),
    )


    fun journey(
        departurePlace: String,
        arrivalPlace: String,
        roadMode: String,
        number: String,
        firstDepartureDatetime: String,
        rollingStockList: String = "commercial_mode:1,commercial_mode:3,commercial_mode:2",
        displayWording: String = "1",
        lang: String = "fr"
    ) = executeGetRequest(
        JourneyResponse.serializer(),
        apiEntryUri.buildUpon().appendPath("journeys.json")
            .appendQueryParameter("departurePlaceXY", departurePlace)
            .appendQueryParameter("arrivalPlaceXY", arrivalPlace)
            .appendQueryParameter("firstDepartureDatetime", firstDepartureDatetime)
            .appendQueryParameter("roadMode", roadMode)
            .appendQueryParameter("rollingStockList", rollingStockList)
            .appendQueryParameter("number", number)
            .appendQueryParameter("displayWording", displayWording)
            .appendQueryParameter("lang", lang).build()
    )
}
