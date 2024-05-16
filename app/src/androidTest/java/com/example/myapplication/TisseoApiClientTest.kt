package com.example.myapplication

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.Dispatchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TisseoApiClientTest {
    // Doc Tisséo : https://data.toulouse-metropole.fr/explore/dataset/api-temps-reel-tisseo/files/72b8051dbf567762260cfa66baf8c333/download/
    @Test
    suspend fun testTisseoPlaces() {
        TisseoApiClient.places("8 allée des sci", dispatcher = Dispatchers.Default)!!.let {
            Log.d("TisseoApiClientTest", "places : $it")
        }
        TisseoApiClient.places("cav", "en", dispatcher = Dispatchers.Default)!!.let {
            Log.d("TisseoApiClientTest", "places : $it")
        }
    }

    @Test
    suspend fun testTisseoApiJourney() {
        val response = TisseoApiClient.journey(
            "basso cambo ",
            "françois verdier",
            "walk",
            "1",
            "2024-05-16 09:30",
            dispatcher = Dispatchers.Default
        )
        Assert.assertNotNull(response)
        println("response :") // Print the response for manual verification
        println(response)
    }
}