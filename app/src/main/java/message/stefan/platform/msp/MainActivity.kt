package message.stefan.platform.msp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import message.stefan.platform.msp.network.MessageDto
import message.stefan.platform.msp.ui.theme.MSPTheme
import androidx.compose.runtime.livedata.observeAsState


class MainActivity : ComponentActivity() {
    private val vm: MessageViewModel by viewModels()  // AndroidViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MSPTheme {
                ConversaApp(vm)
            }
        }

    }
}

@Composable
fun ConversaApp(vm: MessageViewModel) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }

    // LiveData → Compose State
    val loginResult by vm.loginState.observeAsState()
    val messages    by vm.messages.observeAsState(emptyList())


    // Om vi redan har token, hoppa direkt till meddelanden
    val savedToken = session.fetchToken()

    // När login lyckas: ladda meddelanden automatiskt
    // Endast ett ställe som laddar om meddelanden när token finns
    LaunchedEffect(savedToken) {
        if (!savedToken.isNullOrBlank()) {
            vm.loadMessages()
        }
    }


    Box(Modifier.fillMaxSize()) {
        if (savedToken.isNullOrBlank()) {
            // Visa login
            LoginScreen(
                loginResult = loginResult,
                onLogin = { user, pass ->
                    vm.login(user, pass)
                }
            )
        } else {
            // Visa meddelande‑flow
            MessageScreen(
                messages = messages,
                onDelete  = { msgDto -> vm.deleteMessage(msgDto.id) },
                onAdd     = { title, text, image ->
                    vm.addMessage(title, text, image)
                },
                onRefresh = { vm.loadMessages() },
                onLogout  = {
                    session.clearSession()
                    // Rensa VM‑state om du vill:
                    vm.messages.postValue(emptyList())
                    vm.logout()
                }
            )
        }

    }
}


@Composable
fun LoginScreen(
    loginResult: Result<String>?,
    onLogin: (String, String) -> Unit
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Användarnamn") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Lösenord") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onLogin(user, pass) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logga in")
        }
        // Visa spinner eller loader om inloggningen pågår
        loginResult?.let {
            if (it.isFailure) {
                Text(
                    text = it.exceptionOrNull()?.localizedMessage ?: "Okänt fel",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MessageScreen(
    messages: List<MessageDto>,
    onDelete: (MessageDto) -> Unit,
    onAdd:    ( title: String, text: String, image: String) -> Unit,
    onRefresh: () -> Unit,
    onLogout:  () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Meddelanden", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onLogout) {
                Text("Logga ut")
            }
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.End).padding(8.dp)
        ) {
            Text("Ladda om")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(msg.title, style = MaterialTheme.typography.titleSmall)
                        Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Av ${msg.display_name} @ ${msg.date}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = { onDelete(msg) }) {
                            Text("Radera")
                        }
                    }
                }
            }
        }

        AddMessageForm(onAdd = onAdd)
    }
}

@Composable
fun AddMessageForm(
    onAdd: (title: String, text: String, image: String) -> Unit
) {
    var title  by remember { mutableStateOf("") }
    var text   by remember { mutableStateOf("") }
    var image  by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titel") }
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Meddelande") }
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = image,
            onValueChange = { image = it },
            label = { Text("Bild-URL (valfritt)") }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            onAdd(title, text, image)
            title = ""; text = ""; image = ""
        }, Modifier.fillMaxWidth()) {
            Text("Skicka post")
        }
    }
}

