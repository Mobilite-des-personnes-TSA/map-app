package com.example.myapplication

import com.example.myapplication.tisseo.TisseoApiClient
import org.junit.Assert
import org.junit.Test

class TisseoApiClientTest {
    // Doc Tisséo : https://data.toulouse-metropole.fr/explore/dataset/api-temps-reel-tisseo/files/72b8051dbf567762260cfa66baf8c333/download/
    @Test
    fun testTisseoAutocomplete() {
        val client = TisseoApiClient()
        val response = client.apiAutocompleteXml("8 allée des sci")
        Assert.assertNotNull(response)
        println("response :") // Print the response for manual verification
        println(response)
    }

    @Test
    fun testTisseoApiJourney() {
        val client = TisseoApiClient()
        val response = client.apiJourney(
            "xml", "basso cambo ",
            "françois verdier", "walk", "1"
        )
        Assert.assertNotNull(response)
        println("response :") // Print the response for manual verification
        println(response)
    }
}