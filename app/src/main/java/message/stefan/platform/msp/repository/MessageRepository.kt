package message.stefan.platform.msp.repository

import message.stefan.platform.msp.network.RetrofitInstance
import message.stefan.platform.msp.network.MessageDto
import message.stefan.platform.msp.network.ApiResponse
import message.stefan.platform.msp.network.ApiService
import retrofit2.Response
class MessageRepository(private val api: ApiService) {
    suspend fun login(username: String, password: String) =
        api.validateUser(username = username, password = password)

    suspend fun getAll(token: String) =
        api.getAllMessages(token = token)

    suspend fun add(token: String, author: String, title: String, message: String, image: String) =
        api.addMessage(token = token, author = author, title = title, message = message, image = image)

    suspend fun update(token: String, id: Int, author: String, title: String, message: String, image: String) =
        api.updateMessage(token = token, id = id, author = author, title = title, message = message, image = image)

    suspend fun delete(token: String, id: Int) =
        api.deleteMessage(token = token, id = id)
}
