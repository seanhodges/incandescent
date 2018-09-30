package uk.co.seanhodges.incandescent.feature

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference
import java.net.URL


class ConnectToServer(private val ctx: WeakReference<Context>) : AsyncTask<LightwaveServer, Void, Boolean>() {

    override fun doInBackground(vararg server: LightwaveServer): Boolean {
        try {
            val token : String = server[0].authenticate("seanhodges84@gmail.com", "Do4lovedo4love")
            server[0].connect(token)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Connection failed", e)
            return false
        }
        return true
    }

    override fun onPostExecute(success: Boolean) {
        if (!success && ctx.get() != null) {
            Toast.makeText(ctx.get(), "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
        }
        else if (ctx.get() != null) {
            Toast.makeText(ctx.get(), "Connected to Lightwave server :)", Toast.LENGTH_SHORT).show()
        }
    }
}