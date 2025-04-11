package message.stefan.platform.msp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author: String,
    val message: String,
    val imageUri: String? = null
)
