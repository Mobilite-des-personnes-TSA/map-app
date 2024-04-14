package com.example.myapplication.tisseo

import android.util.Log
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class TisseoApiClient {
    private val httpClient  = HttpClients.createDefault()

    @Throws(IOException::class)
    fun executeGetRequest(service: String, format: String, parameters: String?): String? {
        val url = buildUrl(service, format, parameters)
        val httpGet = HttpGet(url)
        val response = httpClient.execute(httpGet)
        val entity = response.entity
        return if (entity != null) {
            EntityUtils.toString(entity, "UTF-8")
        } else {
            null
        }
    }

    private fun buildUrl(service: String, format: String, parameters: String?): String {
        val urlBuilder = StringBuilder(ENTRY_POINT)
        urlBuilder.append(service).append(".").append(format).append("?")
        if (!parameters.isNullOrEmpty()) {
            val params =
                parameters.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (param in params) {
                val keyValue =
                    param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (keyValue.size == 2) {
                    try {
                        urlBuilder.append(URLEncoder.encode(keyValue[0], "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(keyValue[1], "UTF-8"))
                            .append("&")
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        urlBuilder.append("key=").append(API_KEY)
        return urlBuilder.toString()
    }

    fun apiAutocompleteJson(parameter: String): String? {  // return Json with results for autocompletion
        return apiAutocomplete("json", parameter)
    }

    fun apiAutocompleteXml(parameter: String): String? {  // return XML with results for autocompletion
        return apiAutocomplete("xml", parameter)
    }

    private fun apiAutocomplete(format: String, parameter: String): String? {
        return try {
            executeGetRequest("places", format, "term=$parameter")
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun apiJourney(
        format: String, departurePlace: String, arrivalPlace: String,
        roadMode: String, number: String
    ): String? {
        return try {
            executeGetRequest(
                "journeys", format,
                "departurePlace=" + departurePlace + "&arrivalPlace=" + arrivalPlace +
                        "&roadMode=" + roadMode + "&number=" + number +
                        "&displayWording=1" + "&lang=fr"
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val ENTRY_POINT = "https://api.tisseo.fr/v2/"
    }
}
