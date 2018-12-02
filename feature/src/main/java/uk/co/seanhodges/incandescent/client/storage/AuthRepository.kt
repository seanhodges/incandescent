package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedResult
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedTokens
import java.lang.ref.WeakReference
import java.util.*

private const val PREFS_NAME : String = "Incandescent.Prefs.Auth"
private const val PREFS_VERSION : Int = 2

class AuthRepository(private val ctxRef: WeakReference<Context>) {

    fun save(details: LWAuthenticatedResult) {
        ctxRef.get()?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                    .putInt("prefsVersion", PREFS_VERSION)
                    .putString("userId", details._id)
                    .putString("deviceId", UUID.randomUUID().toString())
                    .putString("givenName", details.givenName)
                    .putString("familyName", details.familyName)
                    .putString("accessToken", details.tokens.accessToken)
                    .putString("idToken", details.tokens.idToken)
                    .putString("refreshToken", details.tokens.refreshToken)
                    .putString("tokenType", details.tokens.tokenType)
                    .putLong("expiresIn", details.tokens.expiresIn)
                    .putLong("createdOn", System.currentTimeMillis())
                    .apply()
        }
    }

    fun isAuthenticated(): Boolean {
        val prefs = ctxRef.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs != null && prefs.contains("accessToken") && prefs.contains("refreshToken")
    }

    fun isExpired(): Boolean {
        val prefs = ctxRef.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("accessToken")) {
            return true
        }

        val expiresIn : Long = prefs.getLong("expiresIn", 0)
        val createdOn : Long = prefs.getLong("createdOn", 0)
        val now : Long = System.currentTimeMillis()
        return now > (createdOn + expiresIn)
    }

    fun getCredentials(): Credentials {
        val prefs = ctxRef.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Credentials(
                prefs.getString("accessToken", null)!!,
                prefs.getString("refreshToken", null)!!,
                prefs.getString("deviceId", null)!! // Added for convenience
        )
    }

    fun updateCredentials(tokens: LWAuthenticatedTokens) {
        ctxRef.get()?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                    .putString("accessToken", tokens.accessToken)
                    .putString("refreshToken", tokens.refreshToken)
                    .putLong("createdOn", System.currentTimeMillis())
                    .apply()
        }
    }

    fun getUserId(): String {
        val prefs = ctxRef.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("userId", null)!!
    }

    fun getDeviceId(): String {
        val prefs = ctxRef.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("deviceId", null)!!
    }
}

data class Credentials(
        val accessToken: String,
        val refreshToken: String,
        val deviceId: String
)