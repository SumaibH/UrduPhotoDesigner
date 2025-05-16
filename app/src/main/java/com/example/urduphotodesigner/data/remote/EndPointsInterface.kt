package com.example.urduphotodesigner.data.remote

import com.example.urduphotodesigner.data.model.FontsResponse
import com.example.urduphotodesigner.data.model.ImageResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface EndPointsInterface{
    @GET("fonts")
    suspend fun getAllFonts(
        @Header("X-API-KEY") apiKey: String = "21|kxJ7qhe4kjxjhfzQs4JWG34Pv8DeuIy0ZACTFe7Y5672dc67"
    ): FontsResponse

    @GET("images")
    suspend fun getAllImages(
        @Header("X-API-KEY") apiKey: String = "21|kxJ7qhe4kjxjhfzQs4JWG34Pv8DeuIy0ZACTFe7Y5672dc67"
    ): ImageResponse
}