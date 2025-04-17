package message.stefan.platform.msp

data class Message(
    val id: Int = 0,
    val author: String,
    val message: String,
    val imageUri: String? = null
)

