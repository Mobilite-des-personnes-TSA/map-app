package com.example.myapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.tisseo.TisseoApiClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TisseoApiClientTest {
    // Doc Tisséo : https://data.toulouse-metropole.fr/explore/dataset/api-temps-reel-tisseo/files/72b8051dbf567762260cfa66baf8c333/download/
    @Test
    fun testTisseoAutocomplete() {
        val client = TisseoApiClient()
        val response = client.apiAutocomplete("8 allée des sci")
        Assert.assertNotNull(response)
        println("response :") // Print the response for manual verification
        println(response)
    }

    @Test
    fun testTisseoApiJourney() {
        val client = TisseoApiClient()
        val response = client.apiJourney("basso cambo ",
            "françois verdier", "walk", "1"
        )
        Assert.assertNotNull(response)
        println("response :") // Print the response for manual verification
        println(response)
    }
}