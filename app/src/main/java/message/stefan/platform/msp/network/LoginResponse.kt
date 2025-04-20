package message.stefan.platform.msp.network

data class LoginResponse(
    val status: String,
    val message: String,
    val token: String,
    val id: Int,
    val admin: Boolean
)