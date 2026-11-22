package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApiService {
    @POST(".")
    suspend fun executeQuery(@Body body: AniListGraphQlRequest): Response<AniListResponse>
}
