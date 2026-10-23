package com.example.etapa1.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkClientTest {

    @Test
    fun normalizeBaseUrl_addsHttpsPrefixIfMissing() {
        val result = NetworkClient.normalizeBaseUrl("my-server.trycloudflare.com")
        assertEquals("https://my-server.trycloudflare.com/api/v1/", result)
    }

    @Test
    fun normalizeBaseUrl_preservesHttpScheme() {
        val result = NetworkClient.normalizeBaseUrl("http://10.0.2.2:8000")
        assertEquals("http://10.0.2.2:8000/api/v1/", result)
    }

    @Test
    fun normalizeBaseUrl_alreadyWithApiV1Ending() {
        val result = NetworkClient.normalizeBaseUrl("http://10.0.2.2:8000/api/v1/")
        assertEquals("http://10.0.2.2:8000/api/v1/", result)
    }

    @Test
    fun normalizeBaseUrl_alreadyWithApiV1WithoutTrailingSlash() {
        val result = NetworkClient.normalizeBaseUrl("http://10.0.2.2:8000/api/v1")
        assertEquals("http://10.0.2.2:8000/api/v1/", result)
    }

    @Test
    fun normalizeBaseUrl_cloudflareTunnel() {
        val result = NetworkClient.normalizeBaseUrl("https://dormitory-landlady-detract.ngrok-free.dev/api/v1/")
        assertEquals("https://dormitory-landlady-detract.ngrok-free.dev/api/v1/", result)
    }
}
