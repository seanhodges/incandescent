package uk.co.seanhodges.incandescent.client

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LWAuthenticatedTokens
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

private const val EXECUTOR_NAME : String = "Incandescent.Operation.Executor"
private const val EXECUTOR_FREQUENCY : Long = 1000 // Poll frequency

class OperationExecutor(
        private val server : LightwaveServer,
        private var loadItemIdToFeatureId : MutableMap<Int, String> = mutableMapOf(),
        private val handlerThread: HandlerThread = HandlerThread(EXECUTOR_NAME),
        private val loadQueue: LinkedBlockingDeque<String> = LinkedBlockingDeque<String>(),
        private val changeQueue: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
) : LWEventListener {

    private lateinit var authRepository: AuthRepository
    private var handler: Handler? = null

    private var senderId: String = ""

    var reportHandler: (packet: String) -> Unit? = {}

    @Volatile var started: Boolean = false
    @Volatile var authenticated: Boolean = false

    init {
        server.addListener(this)
    }

    fun enqueueLoad(featureId : String) {
        loadQueue.add(featureId)
    }

    fun enqueueLoadAll(featureIds : List<String>) {
        loadQueue.addAll(featureIds)
    }

    fun enqueueChange(featureId : String, newValue : Int) {
        // Will replace the old value if exists
        changeQueue[featureId] = newValue
    }

    override fun onEvent(event: LWEvent) {
        if (event.clazz.equals("user") && event.operation.equals("authenticate")) {
            authenticated = true
        }
    }

    override fun onRawEvent(packet: String) {
        // TODO(sean): This is hacky, we should decouple all the report gathering stuff
        // TODO(sean): This indirection is only necessary to get activity context to save the reports
        reportHandler.invoke(packet)
    }

    override fun onError(error: Throwable) {
        //TODO(sean): implement proper in-app error handling
        Log.e(javaClass.name, "Server auth error: " + error.message)
    }

    fun start(authRepository: AuthRepository, authHandler: AuthenticationAware) {
        Log.d(javaClass.name, "CommandExecutor started")
        this.authRepository = authRepository
        if (!authRepository.isAuthenticated()) {
            authHandler.onAuthenticationFailed()
        }
        if (!started) {
            started = true
            handlerThread.start()
            handler = Handler(handlerThread.looper)
        }
        handler?.removeCallbacks(eventLoop)
        handler?.post(eventLoop)
    }

    private fun connectToServer() {
        var auth = authRepository.getCredentials()
        var needsReconnect = !server.socketActive
        if (authRepository.isExpired()) {
            try {
                Log.d(javaClass.name, "Refreshing access token using refresh token $auth.refreshToken...")
                val tokens: LWAuthenticatedTokens = server.refreshToken(auth.refreshToken)
                auth = authRepository.updateCredentials(tokens)
                Log.d(javaClass.name, "New access token is: ${auth.accessToken}")
                needsReconnect = true
            } catch (e: Exception) {
                Log.e(javaClass.name, "Connection failed", e)
                throw Exception("Failed to authenticate", e)
            }
        }
        if (needsReconnect) {
            try {
                Log.d(javaClass.name, "Connecting...")
                server.connect(auth.accessToken, senderId)
            } catch (e: Exception) {
                Log.e(javaClass.name, "Connection failed", e)
                throw Exception("Failed to authenticate", e)
            }
        }
        else {
            // We've already authenticated, ignore request
        }
        senderId = authRepository.getDeviceId()
    }

    fun stop() {
        authenticated = false
        handler?.removeCallbacks(eventLoop)
        server.disconnect()
    }

    private val eventLoop = object : Runnable {
        override fun run() {
            if (authenticated) {
                processOperations()
            }
            else {
                connectToServer()
            }
            handler?.postDelayed(this, EXECUTOR_FREQUENCY)
        }
    }

    fun processOperations() {
        while (!loadQueue.isEmpty()) {
            val featureId = loadQueue.take()
            doProcessLoad(featureId)
        }

        changeQueue.entries.forEach { entry ->
            doProcessChange(entry.key, entry.value)
        }
        changeQueue.clear()
    }

    private fun doProcessLoad(featureId: String) {
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

    private fun doProcessChange(featureId: String, value: Int) {
        try {
            val operation = LWOperation("feature", senderId, "write")
            operation.addPayload(LWOperationPayloadFeature(featureId, value))
            server.command(operation)
        }
        catch (e: Throwable) {
            Log.e(javaClass.name, "Server error while processing change operation", e)
        }
    }
}

interface AuthenticationAware {

    fun onAuthenticationSuccess() {}
    fun onAuthenticationFailed() {}

}
