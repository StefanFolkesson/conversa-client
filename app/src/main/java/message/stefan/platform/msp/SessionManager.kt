// SessionManager.kt
package message.stefan.platform.msp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs = context
        .getSharedPreferences("conversa_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit()
        .putString("auth_token", token)
        .apply()

    fun fetchToken(): String? =
        prefs.getString("auth_token", null)

    fun clearToken() = prefs.edit()
        .remove("auth_token")
        .apply()
}
