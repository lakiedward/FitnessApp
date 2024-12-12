package com.example.fitnessapp
//
//import retrofit2.Call
//import retrofit2.http.Body
//import retrofit2.http.Headers
//import retrofit2.http.POST
//
//data class ChatRequest(
//    val model: String = "gpt-3.5-turbo",
//    val messages: List<Message>
//)
//
//data class Message(
//    val role: String,
//    val content: String
//)
//
//data class ChatResponse(
//    val choices: List<Choice>
//)
//
//data class Choice(
//    val message: Message
//)
//
//interface ApiService {
//    @Headers("Content-Type: application/json")
//    @POST("v1/chat/completions")
//    fun sendMessage(
//        @Body request: ChatRequest
//    ): Call<ChatResponse>
//}
