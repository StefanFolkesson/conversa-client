package message.stefan.platform.msp.network

data class MessageDto(
    val id: Int,
    val display_name: String,
    val title: String,
    val message: String,
    val image: String,
    val date: String,
    val author: String
)
