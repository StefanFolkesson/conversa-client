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

    fun saveUserId(id: Int) = prefs.edit()
        .putInt("user_id", id)
        .apply()

    fun fetchUserId(): Int =
        prefs.getInt("user_id", -1) // -1 = inget sparat

    fun saveIsAdmin(isAdmin: Boolean) = prefs.edit()
        .putBoolean("is_admin", isAdmin)
        .apply()

    fun fetchIsAdmin(): Boolean =
        prefs.getBoolean("is_admin", false)

    fun clearSession() = prefs.edit()
        .remove("auth_token")
        .remove("user_id")
        .remove("is_admin")
        .apply()
}
