package message.stefan.platform.msp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import message.stefan.platform.msp.network.MessageDto
import message.stefan.platform.msp.repository.MessageRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import message.stefan.platform.msp.network.RetrofitInstance

class MessageViewModel(app: Application) : AndroidViewModel(app) {
    // Nu får vi Application via getApplication()
    private val session = SessionManager(getApplication())
    private val repo    = MessageRepository(RetrofitInstance.api)

    // Exponerade LiveData
    val loginState   = MutableLiveData<Result<String>>()          // token eller fel
    val messages     = MutableLiveData<List<MessageDto>>()        // din lista
    val errorMessage = MutableLiveData<String>()                  // generella fel

    fun login(username: String, password: String) {
        viewModelScope.launch {
            runCatching {
                val resp = repo.login(username, password)
                if (resp.isSuccessful && resp.body()?.status == "success") {
                    // Här plockar vi ut token‑fältet direkt
                    resp.body()!!.token
                } else {
                    // Om backend svarar fel – kasta meddelandet som exception
                    throw RuntimeException(resp.body()?.message
                        ?: resp.message())
                }
            }.onSuccess { token ->
                // Spara token i SharedPreferences
                session.saveToken(token)
                // Posta resultatet så Compose/UI vet att inloggningen lyckades
                loginState.postValue(Result.success(token))
            }.onFailure { ex ->
                // Posta felet så UI kan visa ett meddelande
                loginState.postValue(Result.failure(ex))
            }
        }
    }

    /** Läs token från SharedPreferences och hämta meddelanden */
    fun loadMessages() {
        val token = session.fetchToken()
        if (token.isNullOrBlank()) {
            errorMessage.postValue("Ingen giltig token, logga in först.")
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
                errorMessage.postValue(ex.localizedMessage)
            }
        }
    }

    /** Exempel på add/update/delete-metoder */
    fun addMessage(author: String, title: String, message: String, image: String) {
        val token = session.fetchToken() ?: return
        viewModelScope.launch {
            val resp = repo.add(token, author, title, message, image)
            if (resp.isSuccessful) {
                loadMessages()
            } else {
                errorMessage.postValue("Failed to add message: ${resp.message()}")
            }
        }
    }
    fun updateMessage(id: Int, author: String, title: String, message: String, image: String) {
        val token = session.fetchToken() ?: return

        viewModelScope.launch {
            val resp = repo.update(token, id, author, title, message, image)
            if (resp.isSuccessful) {
                loadMessages()
            } else {
                errorMessage.postValue("Failed to update message: ${resp.message()}")
            }
        }
    }
    fun deleteMessage(id: Int) {
        val token = session.fetchToken() ?: return
        viewModelScope.launch {
            val resp = repo.delete(token, id)
            if (resp.isSuccessful) {
                loadMessages()
            } else {
                errorMessage.postValue("Failed to delete message: ${resp.message()}")
            }
        }
    }
}
