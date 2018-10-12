package uk.co.seanhodges.incandescent.client

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import java.util.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedResult
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class OperationExecutor(
        private val server : LightwaveServer
) : LWEventListener {

    private val handlerThread = HandlerThread(EXECUTOR_NAME)
    private val loadQueue = ArrayList<String>()
    private val changeQueue = HashMap<String, Int>()

    init {
        server.addListener(this)
    }

    fun enqueueLoad(featureId : String) {
        loadQueue.add(featureId)
    }

    fun enqueueChange(featureId : String, newValue : Int) {
        // Will replace the old value if exists
        changeQueue[featureId] = newValue
    }

    fun connectToServer(ctx: WeakReference<Context>, auth: Map<String, *>) {
        if (!auth.containsKey("accessToken") || isExpired(auth)) {
            val refreshTask = RefreshTokenAndConnectToServerTask(ctx, auth["user"] as String, auth["pass"] as String)
            refreshTask.execute(server)
        }
        else {
            val connectTask = ConnectToServerTask(ctx, auth["accessToken"] as String)
            connectTask.execute(server)
        }
    }

    private fun isExpired(auth: Map<String, *>): Boolean {
        if (!auth.containsKey("expiresIn") || !auth.containsKey("createdOn")) {
            return false
        }

        val expiresIn : Long = auth["expiresIn"] as Long
        val createdOn : Long = auth["createdOn"] as Long
        val now : Long = System.currentTimeMillis()
        return now > (createdOn + expiresIn)
    }

    override fun onEvent(event: LWEvent) {
        if (!(event.clazz.equals("user") && event.operation.equals("authenticate"))) {
            return // Watch for authentication success
        }

        start()
    }

    override fun onError(error: Throwable) {
        //TODO(sean): implement proper in-app error handling
        Log.e(javaClass.name, "Server auth error: " + error.message)
    }

    private fun start() {
        if (handlerThread.isAlive) {
            return
        }

        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        val runnableCode = object : Runnable {
            override fun run() {
                processOperations()
                handler.postDelayed(this, EXECUTOR_FREQUENCY)
            }
        }
        handler.post(runnableCode)
    }

    private fun processOperations() {
        loadQueue.forEach { featureId: String ->
            try {
                val operation = LWOperation("feature", "read")
                operation.addPayload(LWOperationPayloadFeature(featureId))
                server.command(operation)
            }
            catch (e: Throwable) {
                Log.e(javaClass.name, "Server error while processing load operation", e)
            }
        }
        loadQueue.clear()

        changeQueue.keys.forEach { featureId: String ->
            try {
                val newValue: Int? = changeQueue[featureId]
                val operation = LWOperation("feature", "write")
                operation.addPayload(LWOperationPayloadFeature(featureId, newValue!!))
                server.command(operation)}
            catch (e: Throwable) {
                Log.e(javaClass.name, "Server error while processing change operation", e)
            }
        }
        changeQueue.clear()
    }

    companion object {
        private val EXECUTOR_NAME : String = "Incandescent.Operation.Executor"
        private val EXECUTOR_FREQUENCY : Long = 1000 // Poll frequency
    }
}

class RefreshTokenAndConnectToServerTask(private val ctx: WeakReference<Context>, private val user: String, private val pass: String) : AsyncTask<LightwaveServer, Void, LWAuthenticatedResult>() {

    override fun doInBackground(vararg server: LightwaveServer): LWAuthenticatedResult? {
        try {
            // FIXME: We don't know the refresh token endpoint so we have to obtain a fresh token
            Log.d(javaClass.name, "Refreshing access token...")
            val authResult: LWAuthenticatedResult = server[0].authenticate(user, pass)
            Log.d(javaClass.name, "Access token is: ${authResult.tokens.accessToken}")
            Log.d(javaClass.name, "Connecting...")
            server[0].connect(authResult.tokens.accessToken)
            return authResult
        } catch (e: Exception) {
            Log.e(javaClass.name, "Connection failed", e)
        }
        return null
    }

    override fun onPostExecute(result: LWAuthenticatedResult?) {
        super.onPostExecute(result)

        if (result == null && ctx.get() != null) {
            Toast.makeText(ctx.get(), "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
        }
        else if (ctx.get() != null) {
            val prefs = ctx.get()!!.getSharedPreferences("userDetails", Context.MODE_PRIVATE)
            prefs.edit()
                    .putString("accessToken", result!!.tokens.accessToken)
                    .putString("idToken", result.tokens.idToken)
                    .putString("refreshToken", result.tokens.refreshToken)
                    .putString("tokenType", result.tokens.tokenType)
                    .putString("user", prefs.getString("user", ""))
                    .putString("pass", prefs.getString("pass", ""))
                    .putLong("expiresIn", result.tokens.expiresIn)
                    .putLong("createdOn", System.currentTimeMillis())
                    .apply()

            Toast.makeText(ctx.get(), "Connected to Lightwave server :)", Toast.LENGTH_SHORT).show()
        }
    }
}

class ConnectToServerTask(private val ctx: WeakReference<Context>, private val accessToken: String) : AsyncTask<LightwaveServer, Void, Boolean>() {

    override fun doInBackground(vararg server: LightwaveServer): Boolean {
        try {
            Log.d(javaClass.name, "Connecting...")
            server[0].connect(accessToken)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Connection failed", e)
            return false
        }
        return true
    }

    override fun onPostExecute(success: Boolean) {
        super.onPostExecute(success)

        if (!success && ctx.get() != null) {
            Toast.makeText(ctx.get(), "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
        }
        else if (ctx.get() != null) {
            Toast.makeText(ctx.get(), "Connected to Lightwave server :)", Toast.LENGTH_SHORT).show()
        }
    }
}