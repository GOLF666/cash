package com.example.cash

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GptApiService {

    @Headers(
        "Content-Type: application/json",
        "api-key: BQfMSxoqkw69KfVYDLGmpeLI8YTgQCBHO1gqcEzb1MhtJEgxLDMsJQQJ99BEACfhMk5XJ3w3AAAAACOGHyk9"
    )
    @POST("openai/deployments/gpt-4o/chat/completions?api-version=2024-05-01-preview")
    fun sendMessage(@Body request: GptRequest): Call<GptResponse>
}
