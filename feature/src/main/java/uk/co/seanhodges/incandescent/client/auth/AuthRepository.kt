package uk.co.seanhodges.incandescent.client.auth

import android.content.Context
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedResult
import java.lang.ref.WeakReference

class AuthRepository(private val ctx: WeakReference<Context>) {

    fun save(details: LWAuthenticatedResult, user: String? = null, pass: String? = null) {
        val prefs = ctx.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
                .putInt("prefsVersion", PREFS_VERSION)
                .putString("id", details._id)
                .putString("givenName", details.givenName)
                .putString("familyName", details.familyName)
                .putString("accessToken", details.tokens.accessToken)
                .putString("idToken", details.tokens.idToken)
                .putString("refreshToken", details.tokens.refreshToken)
                .putString("tokenType", details.tokens.tokenType)
                .putString("user", user ?: prefs.getString("user", ""))
                .putString("pass", pass ?: prefs.getString("pass", ""))
                .putLong("expiresIn", details.tokens.expiresIn)
                .putLong("createdOn", System.currentTimeMillis())
                .apply()
    }

    fun isAuthenticated(): Boolean {
        val prefs = ctx.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs != null && prefs.contains("accessToken")
    }


    fun isExpired(): Boolean {
        val prefs = ctx.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("accessToken")) {
            return true
        }

        val expiresIn : Long = prefs.getLong("expiresIn", 0)
        val createdOn : Long = prefs.getLong("createdOn", 0)
        val now : Long = System.currentTimeMillis()
        return now > (createdOn + expiresIn)
    }

    fun getCredentials(): Credentials {
        val prefs = ctx.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Credentials(
            prefs.getString("user", "")!!,
            prefs.getString("pass", "")!!
        )
    }

    fun getAccessToken(): String {
        val prefs = ctx.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("accessToken", null)!!
    }

    companion object {
        private const val PREFS_NAME : String = "Incandescent.Prefs.Auth"
        private const val PREFS_VERSION : Int = 2
    }
}

data class Credentials(
        val user: String,
        val pass: String
)