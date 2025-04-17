package message.stefan.platform.msp.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Inloggning
    @GET("conversa.php")
    suspend fun validateUser(
        @Query("validate") validate: String = "1",
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<LoginResponse>

    // Hämta alla meddelanden
    @GET("conversa.php")
    suspend fun getAllMessages(
        @Query("getAll") getAll: String = "1",
        @Query("token") token: String
    ): Response<List<MessageDto>>

    // Lägg till ett meddelande
    @FormUrlEncoded
    @POST("conversa.php")
    suspend fun addMessage(
        @Query("validate") validate: String = "1",
        @Query("token")    token:    String,
        @Field("add")      add:      String = "1",
        @Field("data[author]")  author: String,
        @Field("data[title]")   title:  String,
        @Field("data[message]") message:String,
        @Field("data[image]")   image:  String
    ): Response<ApiResponse>

    // Uppdatera
    @FormUrlEncoded
    @POST("conversa.php")
    suspend fun updateMessage(
        @Query("validate") validate: String = "1",
        @Query("token")    token:    String,
        @Field("update")   update:   String = "1",
        @Field("id")       id:       Int,
        @Field("data[author]")  author: String,
        @Field("data[title]")   title:  String,
        @Field("data[message]") message:String,
        @Field("data[image]")   image:  String
    ): Response<ApiResponse>

    // Radera
    @FormUrlEncoded
    @POST("conversa.php")
    suspend fun deleteMessage(
        @Query("validate") validate: String = "1",
        @Query("token")    token:    String,
        @Field("delete")   delete:   String = "1",
        @Field("id")       id:       Int
    ): Response<ApiResponse>
}
