package message.stefan.platform.msp

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "message-database"
        ).build()
    }

    // Lista som innehåller meddelanden för UI:t.
    val messages = mutableStateListOf<Message>()

    init {
        // Ladda befintliga meddelanden från databasen på en IO-tråd
        viewModelScope.launch {
            val messageEntities = withContext(Dispatchers.IO) {
                db.messageDao().getAllMessagesNonSuspend()
            }
            // Uppdatera UI:t på main-tråden
            messages.addAll(messageEntities.map {
                Message(it.id, it.author, it.message, it.imageUri)
            })
        }
    }

    // Funktion för att lägga till ett meddelande både i UI-listan och i databasen.
    fun addMessage(message: Message) {
        messages.add(message)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.messageDao().insertMessageNonSuspend(
                    MessageEntity(
                        author = message.author,
                        message = message.message,
                        imageUri = message.imageUri
                    )
                )
            }
        }
    }
    fun deleteMessage(message: Message) {
        // Ta bort meddelandet från UI-listan
        messages.remove(message)
        // Ta bort meddelandet från databasen
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.messageDao().deleteMessageNonSuspend(
                    MessageEntity(message.id, message.author, message.message, message.imageUri)
                )
            }
        }
    }

}
