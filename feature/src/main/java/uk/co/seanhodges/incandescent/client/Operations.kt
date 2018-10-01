package uk.co.seanhodges.incandescent.client

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import java.util.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class OperationExecutor(
        private val server : LightwaveServer
) {

    private val handlerThread = HandlerThread(EXECUTOR_NAME)
    private val operationQueue = HashMap<String, Int>()

    fun enqueue(featureId : String, newValue : Int) {
        // Will replace the old value if exists
        operationQueue[featureId] = newValue
    }

    fun connectToServer(ctx: WeakReference<Context>) {
        val connectTask = ConnectToServerTask(ctx, onConnected = { success: Boolean ->
            // Start operation executor
            if (success) {
                start()
            }
        })
        connectTask.execute(server)
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
        operationQueue.keys.forEach { featureId: String ->
            val newValue : Int? = operationQueue[featureId]
            val operation = LWOperation("feature", "write")
            operation.addPayload(LWOperationPayloadFeature(featureId, newValue!!))
            server.command(operation)
        }
        operationQueue.clear()
    }

    companion object {
        private val EXECUTOR_NAME : String = "Incandescent.Operation.Executor"
        private val EXECUTOR_FREQUENCY : Long = 1000 // Poll frequency
    }
}

class ConnectToServerTask(private val ctx: WeakReference<Context>,
                          private val onConnected: (success: Boolean) -> Unit
) : AsyncTask<LightwaveServer, Void, Boolean>() {

    override fun doInBackground(vararg server: LightwaveServer): Boolean {
        try {
            Log.d(javaClass.name, "Connecting...")
            val token : String = server[0].authenticate("seanhodges84@gmail.com", "Do4lovedo4love")
            Log.d(javaClass.name, "Access token is: $token")
            server[0].connect(token)
            Log.d(javaClass.name, "Connection success")
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
        Log.d(javaClass.name, "Notifying caller")
        onConnected.invoke(success)
    }
}