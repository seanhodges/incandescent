package uk.co.seanhodges.incandescent.client

import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import uk.co.seanhodges.incandescent.client.auth.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.Credentials
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import java.util.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedResult
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer


class OperationExecutor(
        private val server : LightwaveServer
) : LWEventListener {

    private val handlerThread = HandlerThread(EXECUTOR_NAME)
    private val loadQueue = ArrayList<String>()
    private val changeQueue = HashMap<String, Int>()

    private var senderId: String = ""

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

    fun connectToServer(authRepository: AuthRepository, onComplete: (success: Boolean) -> Unit) {
        if (authRepository.isExpired()) {
            val refreshTask = RefreshTokenAndConnectToServerTask(authRepository, onComplete)
            refreshTask.execute(server)
        }
        else {
            val connectTask = ConnectToServerTask(authRepository, onComplete)
            connectTask.execute(server)
        }
        senderId = authRepository.getDeviceId()
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
                val operation = LWOperation("feature", senderId, "read")
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
                val operation = LWOperation("feature", senderId, "write")
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

class RefreshTokenAndConnectToServerTask(
        private val authRepository: AuthRepository,
        private val onComplete: (success: Boolean) -> Unit
) : AsyncTask<LightwaveServer, Void, LWAuthenticatedResult>() {

    override fun doInBackground(vararg server: LightwaveServer): LWAuthenticatedResult? {
        val auth: Credentials = authRepository.getCredentials()
        try {
            // FIXME: We don't know the refresh token endpoint so we have to obtain a fresh token
            Log.d(javaClass.name, "Refreshing access token...")
            val authResult: LWAuthenticatedResult = server[0].authenticate(auth.user, auth.pass)
            Log.d(javaClass.name, "Access token is: ${authResult.tokens.accessToken}")
            Log.d(javaClass.name, "Connecting...")
            server[0].connect(authResult.tokens.accessToken, auth.deviceId)
            return authResult
        } catch (e: Exception) {
            Log.e(javaClass.name, "Connection failed", e)
        }
        return null
    }

    override fun onPostExecute(result: LWAuthenticatedResult?) {
        super.onPostExecute(result)
        if (result != null) {
            authRepository.save(result)
            onComplete(true)
        }
        onComplete(false)
    }
}

class ConnectToServerTask(
        private val authRepository: AuthRepository,
        private val onComplete: (success: Boolean) -> Unit
) : AsyncTask<LightwaveServer, Void, Boolean>() {

    override fun doInBackground(vararg server: LightwaveServer): Boolean {
        try {
            val auth: Credentials = authRepository.getCredentials()
            Log.d(javaClass.name, "Connecting...")
            server[0].connect(auth.accessToken, auth.deviceId)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Connection failed", e)
            return false
        }
        return true
    }

    override fun onPostExecute(success: Boolean) {
        super.onPostExecute(success)
        onComplete(success)
    }
}