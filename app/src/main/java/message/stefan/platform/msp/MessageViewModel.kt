package message.stefan.platform.msp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import message.stefan.platform.msp.network.MessageDto
import message.stefan.platform.msp.repository.MessageRepository
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import message.stefan.platform.msp.network.RetrofitInstance

class MessageViewModel(app: Application) : AndroidViewModel(app) {
    // Nu får vi Application via getApplication()
    private val session = SessionManager(getApplication())
    private val repo    = MessageRepository(RetrofitInstance.api)

    // Exponerade LiveData
    val loginState   = MutableLiveData<Result<String>>()          // token eller fel
    val messages     = MutableLiveData<List<MessageDto>>()        // din lista

    fun login(username: String, password: String) {
        viewModelScope.launch {
            runCatching {
                val resp = repo.login(username, password)
                val body = resp.body()
                if (resp.isSuccessful && body?.status == "success" && body.token.isNotBlank()) {
                    body
                } else {
                    throw RuntimeException(body?.message ?: resp.message())
                }
            }.onSuccess { body ->
                // Spara alla session‑värden
                session.saveToken(body.token)
                session.saveUserId(body.id)
                session.saveIsAdmin(body.admin)

                // Posta token (eller hela body om du vill)
                loginState.postValue(Result.success(body.token))
                loadMessages()
                Toast.makeText(getApplication(), "Logged in", Toast.LENGTH_SHORT).show()
            }.onFailure { ex ->
                loginState.postValue(Result.failure(ex))
            }
        }
    }


    /** Läs token från SharedPreferences och hämta meddelanden */
    fun loadMessages() {
        val token = session.fetchToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(getApplication(), "No token", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            runCatching {
                val resp = repo.getAll(token)
                if (resp.isSuccessful) resp.body()!!
                else throw RuntimeException("Fetch failed: ${resp.message()}")
            }.onSuccess { list ->
                messages.postValue(list)
            }.onFailure { ex ->
                Toast.makeText(getApplication(), "Failed to load messages: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Exempel på add/update/delete-metoder */
    fun addMessage(title: String, message: String, image: String) {
        val token = session.fetchToken() ?: return
        val authorId = session.fetchUserId()
        if (authorId < 0) return
        viewModelScope.launch {
            Log.d("AAA", "token: $token, author: $authorId, title: $title, message: $message, image: $image")
            val resp = repo.add(token, authorId, title, message, image)
            val body = resp.body()
            if (body?.status == "success" && resp.isSuccessful)
                loadMessages()
            else
                Toast.makeText(getApplication(), "Failed to add message: ${body?.message}", Toast.LENGTH_SHORT).show()
        }
    }
    fun updateMessage(id: Int, author: String, title: String, message: String, image: String) {
        val token = session.fetchToken() ?: return
        viewModelScope.launch {
            val resp = repo.update(token, id, author, title, message, image)
            val body = resp.body()
            if (body?.status == "success" && resp.isSuccessful)
                loadMessages()
            else
                Toast.makeText(getApplication(), "Failed to update message: ${body?.message}", Toast.LENGTH_SHORT).show()
        }
    }
    fun deleteMessage(id: Int) {
        val token = session.fetchToken() ?: return
        viewModelScope.launch {
            val resp = repo.delete(token, id)
            val body = resp.body()
            if (body?.status == "success" && resp.isSuccessful)
                loadMessages()
            else
                Toast.makeText(getApplication(), "Failed to delete message: ${body?.message}", Toast.LENGTH_SHORT).show()
        }
    }
    fun logout(){
        val token = session.fetchToken() ?:return
        viewModelScope.launch {
            val resp = repo.logout(token)
            val body = resp.body()
            if (body?.status == "success" && resp.isSuccessful) {
                session.clearSession()

            } else {
                Toast.makeText(
                    getApplication(),
                    "Failed to logout: ${body?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }
}
