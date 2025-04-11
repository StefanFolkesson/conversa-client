package message.stefan.platform.msp
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages")
    fun getAllMessagesNonSuspend(): List<MessageEntity>

    @Insert
    fun insertMessageNonSuspend(message: MessageEntity)

    @Delete
    fun deleteMessageNonSuspend(message: MessageEntity)
}