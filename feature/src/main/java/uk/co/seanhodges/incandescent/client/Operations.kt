package uk.co.seanhodges.incandescent.client

import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import uk.co.seanhodges.incandescent.client.storage.Credentials
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedTokens
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer


class OperationExecutor(
        private val server : LightwaveServer,
        private var loadItemIdToFeatureId : MutableMap<Int, String> = mutableMapOf()
) : LWEventListener {

    private val handlerThread = HandlerThread(EXECUTOR_NAME)
    private val loadQueue = arrayListOf<String>()
    private val changeQueue = mutableMapOf<String, Int>()

    private var senderId: String = ""

    var reportHandler: (packet: String) -> Unit? = {}

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
        else if (!server.socketActive) {
            val connectTask = ConnectToServerTask(authRepository, onComplete)
            connectTask.execute(server)
        }
        else {
            // We've already connected, ignore request
        }
        senderId = authRepository.getDeviceId()
    }

    override fun onEvent(event: LWEvent) {
        if (!(event.clazz.equals("user") && event.operation.equals("authenticate"))) {
            return // Watch for authentication success
        }

        start()
    }

    override fun onRawEvent(packet: String) {
        // TODO(sean): This is hacky, we should decouple all the report gathering stuff
        // TODO(sean): This indirection is only necessary to get active activity context to save the reports
        reportHandler.invoke(packet)
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

    fun processOperations() {
        loadQueue.forEach { featureId: String ->
            try {
                val operation = LWOperation("feature", senderId, "read")
                operation.addPayload(LWOperationPayloadFeature(featureId))

                // We're forcing the transaction ID into the itemId field to handle an API bug:
                // LW doesn't return any reliable reference back for these events, so we don't know which feature the value is for.
                // Using the itemId we can map feature read events back to the feature that originally requested them
                operation.items[0].itemId = operation.transactionId
                loadItemIdToFeatureId[operation.items[0].itemId] = featureId

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
) : AsyncTask<LightwaveServer, Void, LWAuthenticatedTokens>() {

    override fun doInBackground(vararg server: LightwaveServer): LWAuthenticatedTokens? {
        val auth: Credentials = authRepository.getCredentials()
        try {
            Log.d(javaClass.name, "Refreshing access token...")
            val tokens: LWAuthenticatedTokens = server[0].refreshToken(auth.refreshToken)
            if (tokens.refreshToken == null) {
                return null
            }
            Log.d(javaClass.name, "Access token is: ${tokens.accessToken}")
            Log.d(javaClass.name, "Connecting...")
            server[0].connect(tokens.accessToken, auth.deviceId)
            return tokens
        } catch (e: Exception) {
            Log.e(javaClass.name, "Connection failed", e)
        }
        return null
    }

    override fun onPostExecute(result: LWAuthenticatedTokens?) {
        super.onPostExecute(result)
        if (result != null) {
            authRepository.updateCredentials(result)
            onComplete(true)
        }
        else {
            onComplete(false)
        }
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