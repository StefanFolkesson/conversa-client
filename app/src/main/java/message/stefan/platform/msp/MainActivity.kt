package message.stefan.platform.msp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import message.stefan.platform.msp.ui.theme.MSPTheme


class MainActivity : ComponentActivity() {
    // Hämta ViewModel, observera att vi nu använder AndroidViewModel
    private val messageViewModel: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MSPTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    ViewMessages(
                        messages = messageViewModel.messages,
                        onDelete = { messageViewModel.deleteMessage(it) },
                        modifier = Modifier.weight(1f)
                    )
                    AddMessage(messageViewModel, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class Message(
    val id: Int = 0,
    val author: String,
    val message: String,
    val imageUri: String? = null
)
@Composable
fun ViewMessages(
    messages: MutableList<Message>,
    onDelete: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        userScrollEnabled = true
    ) {
        items(messages) { message ->
            ViewMessage(message = message, onDelete = onDelete)
        }
    }
}

@Composable
fun AddMessage(
    viewModel: MessageViewModel,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var forfattare by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Meddelande") }
        )
        TextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Bildadress") }
        )
        TextField(
            value = forfattare,
            onValueChange = { forfattare = it },
            label = { Text("Författare") }
        )
        Button(
            onClick = {
                // Skapa nytt meddelande med id 0 (Room genererar ett nytt id)
                val newMessage = Message(0, forfattare, text, url)
                viewModel.addMessage(newMessage)
                // Töm fälten efter inskickning (om så önskas)
                text = ""
                url = ""
                forfattare = ""
            }
        ) {
            Text("Skicka post")
        }
    }
}
@Composable
fun ViewMessage(message: Message, onDelete: (Message) -> Unit, modifier: Modifier = Modifier) {
    Column {
        Row(
            modifier = modifier
                .padding(2.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = message.author,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .weight(1f),
                fontSize = 12.sp
            )
            Text(
                text = message.message,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp
            )
            Button(onClick = { onDelete(message) }) {
                Text("Radera")
            }
        }
        if (!message.imageUri.isNullOrEmpty()) {
            AsyncImage(
                model = message.imageUri,
                contentDescription = "Meddelandets bild",
                modifier = Modifier
                    .size(100.dp)
                    .padding(top = 4.dp)
            )
        }
    }
}




