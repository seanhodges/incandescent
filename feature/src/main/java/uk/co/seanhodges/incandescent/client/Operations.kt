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

    fun connectToServer(ctx: WeakReference<Context>) {
        val connectTask = ConnectToServerTask(ctx)
        connectTask.execute(server)
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
            val operation = LWOperation("feature", "read")
            operation.addPayload(LWOperationPayloadFeature(featureId))
            server.command(operation)
        }
        loadQueue.clear()

        changeQueue.keys.forEach { featureId: String ->
            val newValue : Int? = changeQueue[featureId]
            val operation = LWOperation("feature", "write")
            operation.addPayload(LWOperationPayloadFeature(featureId, newValue!!))
            server.command(operation)
        }
        changeQueue.clear()
    }

    companion object {
        private val EXECUTOR_NAME : String = "Incandescent.Operation.Executor"
        private val EXECUTOR_FREQUENCY : Long = 1000 // Poll frequency
    }
}

class ConnectToServerTask(private val ctx: WeakReference<Context>) : AsyncTask<LightwaveServer, Void, Boolean>() {

    override fun doInBackground(vararg server: LightwaveServer): Boolean {
        try {
            Log.d(javaClass.name, "Connecting...")
            val token : String = server[0].authenticate("seanhodges84@gmail.com", "Do4lovedo4love")
            Log.d(javaClass.name, "Access token is: $token")
            server[0].connect(token)
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