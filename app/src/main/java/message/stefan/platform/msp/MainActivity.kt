package message.stefan.platform.msp

import android.os.*
import android.os.Bundle
import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import message.stefan.platform.msp.network.MessageDto
import message.stefan.platform.msp.ui.state.UiState
import message.stefan.platform.msp.ui.theme.MSPTheme

class MainActivity : ComponentActivity() {
    private val vm: MessageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
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

    val loginState by vm.loginState.collectAsState()
    val messages by vm.messages.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var tokenState by remember { mutableStateOf(session.fetchToken()) }

    // Update token after login
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            tokenState = session.fetchToken()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            vm.loadMessages()
            delay(15_000) // Every 15 seconds
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp)
        ) {
            if (tokenState.isNullOrBlank()) {
                key(tokenState) {
                    LoginScreen(
                        loginState = loginState,
                        onLogin = { user, pass -> vm.login(user, pass) },
                        snackbarHostState = snackbarHostState
                    )
                }
            } else {
                MessageScreen(
                    messages = messages,
                    onDelete = { msgDto -> vm.deleteMessage(msgDto.id) },
                    onAdd = { title, text, image -> vm.addMessage(title, text, image) },
                    onRefresh = { vm.loadMessages() },
                    onLogout = {
                        session.clearSession()
                        vm.logout()
                        tokenState = null
                    },
                    session = session
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    loginState: UiState<String>,
    onLogin: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    // Use a stable trigger to reset form
    val resetKey = (loginState as? UiState.Success)?.data ?: "fresh"

    var user by remember(resetKey) { mutableStateOf("") }
    var pass by remember(resetKey) { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is UiState.Error) {
            snackbarHostState.showSnackbar(loginState.message)
        }
    }


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

        if (loginState is UiState.Loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MessageScreen(
    messages: List<MessageDto>,
    onDelete: (MessageDto) -> Unit,
    onAdd: (title: String, text: String, image: String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    session: SessionManager
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Lägg till meddelande")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(8.dp)
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

                            msg.image.takeIf { it.isNotBlank() }?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Meddelandets bild",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(top = 4.dp)
                                )
                            }

                            msg.author.toIntOrNull()?.let { authorId ->
                                if (authorId == session.fetchUserId()) {
                                    Button(onClick = { onDelete(msg) }) {
                                        Text("Radera")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddMessageDialog(
            onDismiss = { showDialog = false },
            onSubmit = { title, text, image ->
                onAdd(title, text, image)
                showDialog = false
            }
        )
    }
}

@Composable
fun AddMessageForm(onAdd: (title: String, text: String, image: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titel") })
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Meddelande") })
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = image, onValueChange = { image = it }, label = { Text("Bild-URL (valfritt)") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            onAdd(title, text, image)
            title = ""; text = ""; image = ""
        }, Modifier.fillMaxWidth()) {
            Text("Skicka post")
        }
    }
}
@Composable
fun AddMessageDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, text: String, image: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSubmit(title, text, image)
                title = ""; text = ""; image = ""
            }) {
                Text("Skicka")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        },
        title = { Text("Nytt meddelande") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Meddelande") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("Bild-URL (valfritt)") }
                )
            }
        }
    )
}
fun Application.sendNewMessageNotification(title: String, text: String) {
    val channelId = "msg_alerts"
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Meddelanden",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}


