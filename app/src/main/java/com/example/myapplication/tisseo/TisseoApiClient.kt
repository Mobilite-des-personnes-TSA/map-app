package com.example.myapplication.tisseo

import android.net.Uri
import android.util.Log
import android.window.OnBackInvokedDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// TODO: Possible data Structure
//      See what else is necessary
//  Area system, all the nodes in that area have a high coefficient
//  How do we fetch our accommodation Data ?
data class Road(
    val id: String,
    val name: String,
    val nodes: List<Node>
)

data class Node(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val noiseCoefficient: Double
)

object TisseoApiClient {

    var API_KEY = "blabla API KEY"

    private val apiEntryUri =
        Uri.Builder().scheme("https").authority("api.tisseo.fr").appendPath("v2")
            .appendQueryParameter("key", API_KEY).build()

    /** TODO: Couroutine and flows
     *
     */
    private suspend fun <T> executeGetRequest(deserializer: DeserializationStrategy<T>, uri: Uri, dispatcher: CoroutineDispatcher): T? {
        Log.d("TisseoApiClient", "Request: $uri")

        // Blocking operation
        withContext(dispatcher) {
            val connection = URL(uri.toString()).openConnection() as HttpsURLConnection

            val responseCode = connection.responseCode
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val responseData = try {
                    BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
                } catch (e: IOException) {
                    Log.wtf("TisseoApiClient", e.message, e)
                    null
                } finally {
                    connection.disconnect()
                }
                Log.d("TisseoApiClient", "Response Data: $responseData")
            } else {
                println("API request failed with response code: $responseCode")
            }
        }

        Log.d("TisseoApiClient", "Response Data: $responseData")
        return responseData?.let { Json.decodeFromString(deserializer, it) }
    }


    /** Adapt the Tisseo API response type to ours
     * TODO: Reassess the serialization process
     */
    @ExperimentalSerializationApi
    suspend fun places(
        term: String ="", coordinatesXY : String ="", lang: String = "fr",
        dispatcher: CoroutineDispatcher
    ) = executeGetRequest(
            PlacesResponse.serializer(),
            apiEntryUri.buildUpon().appendPath("places.json")
                .appendQueryParameter("term", term)
                .appendQueryParameter("coordinatesXY", coordinatesXY)
                .appendQueryParameter("lang", lang)
                .build(),
            dispatcher
        )

    /** Constructs a road for the given journey through the Tisséo API
     *  @param departurePlace
     *  @param arrivalPlace
     *  @param roadMode
     *  @param number                                       What's that for ?
     *  @param displayWording                               What's that for ?
     *  @param lang                                         What's that for ?
     */
    suspend fun journey(
        departurePlace: String,
        arrivalPlace: String,
        roadMode: String,
        number: String,
        dispatcher: CoroutineDispatcher,              //   Do I keep this as a dependency ?
        displayWording: String = "1",
        lang: String = "fr"
    ) = executeGetRequest(
            JourneyResponse.serializer(),
            apiEntryUri.buildUpon().appendPath("journeys.json")
                .appendQueryParameter("departurePlace", departurePlace)
                .appendQueryParameter("arrivalPla   ce", arrivalPlace)
                .appendQueryParameter("roadMode", roadMode).appendQueryParameter("number", number)
                .appendQueryParameter("displayWording", displayWording)
                .appendQueryParameter("lang", lang).build(),
            dispatcher
        )
}
