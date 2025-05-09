package message.stefan.platform.msp

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import message.stefan.platform.msp.network.MessageDto
import message.stefan.platform.msp.network.RetrofitInstance
import message.stefan.platform.msp.repository.MessageRepository
import android.util.Log
import message.stefan.platform.msp.ui.state.UiState

class MessageViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionManager(getApplication())
    private val repo    = MessageRepository(RetrofitInstance.api)

    // StateFlows exposed to Compose
    private val _loginState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val loginState: StateFlow<UiState<String>> = _loginState

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            runCatching {
                val resp = repo.login(username, password)
                val body = resp.body()
                if (resp.isSuccessful && body?.status == "success" && body.token.isNotBlank()) {
                    body
                } else {
                    throw RuntimeException(body?.message ?: resp.message())
                }
            }.onSuccess { body ->
                session.saveToken(body.token)
                session.saveUserId(body.id)
                session.saveIsAdmin(body.admin)

                _loginState.value = UiState.Success(body.token)
                loadMessages()
            }.onFailure { ex ->
                _loginState.value = UiState.Error(ex.localizedMessage ?: "Okänt fel")
            }
        }
    }

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
                _messages.value = list
            }.onFailure { ex ->
                Toast.makeText(getApplication(), "Failed to load messages: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addMessage(title: String, message: String, image: String) {
        val token = session.fetchToken() ?: return
        val authorId = session.fetchUserId()
        if (authorId < 0) return
        viewModelScope.launch {
            Log.d("AAA", "token: $token, author: $authorId, title: $title, message: $message, image: $image")
            val resp = repo.add(token, authorId, title, message, image, target = 0)
            val body = resp.body()
            if (body?.status == "success" && resp.isSuccessful)
                loadMessages()
            else
                Toast.makeText(getApplication(), "Failed to add message: ${body?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateMessage(id: Int, author: Int, title: String, message: String, image: String) {
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

    fun logout() {
        val token = session.fetchToken() ?: return
        viewModelScope.launch {
            val resp = repo.logout(token)
            val body = resp.body()
            if (body?.status == "success" && resp.isSuccessful) {
                session.clearSession()
                _messages.value = emptyList()
                _loginState.value = UiState.Idle
            } else {
                Toast.makeText(getApplication(), "Failed to logout: ${body?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
