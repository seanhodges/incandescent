package uk.co.seanhodges.incandescent.client

import android.content.Context
import android.net.*
import android.os.AsyncTask
import android.util.Log
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECTION_CHECK_TIMEOUT = 2000

class ConnectionStateMonitor(
        context: Context,
        listener: ConnectionAware,
        private val executor: OperationExecutor = Inject.executor,
        private val networkRequest: NetworkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()
) : ConnectivityManager.NetworkCallback() {

    private var connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var contextRef: WeakReference<Context> = WeakReference(context)
    private var listenerRef: WeakReference<ConnectionAware> = WeakReference(listener)

    private var state: NetworkInfo.State = NetworkInfo.State.UNKNOWN

    fun resume() {
        Log.d(javaClass.name, "Started listening for network changes")
        connectivityManager.registerNetworkCallback(networkRequest, this)

        // Check on startup...
        IsNetworkAvailableNowTask(this, connectivityManager.activeNetwork).execute()
    }

    fun pause() {
        Log.d(javaClass.name, "Stopped listening for network changes")
        executor.stop()
        connectivityManager.unregisterNetworkCallback(this)
    }

    internal class IsNetworkAvailableNowTask(
            private val parent: ConnectionStateMonitor,
            private val activeNetwork: Network?
    ) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg voids: Void): Boolean {
            try {
                val url = URL("http://clients3.google.com/generate_204")
                        .openConnection() as HttpURLConnection
                url.setRequestProperty("User-Agent", "Android")
                url.setRequestProperty("Connection", "close")
                url.connectTimeout = CONNECTION_CHECK_TIMEOUT
                url.connect()
                return url.responseCode == 204
            } catch (e: IOException) {
                // Internet connection unavailable
                return false
            }
        }

        override fun onPostExecute(result: Boolean) {
            when (result) {
                true -> parent.onAvailable(activeNetwork)
                else -> parent.onUnavailable()
            }
        }
    }

    override fun onLost(network: Network?) {
        if (state == NetworkInfo.State.DISCONNECTED) {
            return
        }
        state = NetworkInfo.State.DISCONNECTED
        contextRef.get()?.let { context ->
            Log.d(javaClass.name, "Lost connection")
            executor.stop()
        }
        listenerRef.get()?.onConnectionLost()
    }

    override fun onUnavailable() {
        if (state == NetworkInfo.State.DISCONNECTED) {
            return
        }
        state = NetworkInfo.State.DISCONNECTED
        contextRef.get()?.let { context ->
            Log.d(javaClass.name, "Connection unavailable")
            executor.stop()
        }
        listenerRef.get()?.onConnectionLost()
    }

    override fun onAvailable(network: Network?) {
        if (state == NetworkInfo.State.CONNECTED) {
            return
        }
        state = NetworkInfo.State.CONNECTED
        contextRef.get()?.let { context ->
            Log.d(javaClass.name, "Connection available")
            val authRepository = AuthRepository(WeakReference(context))
            listenerRef.get()?.let { listener ->
                executor.start(authRepository, listener)
            }
        }
        listenerRef.get()?.onConnectionAvailable()
    }

}

interface ConnectionAware : AuthenticationAware {

    fun onConnectionAvailable() {}
    fun onConnectionLost() {}

}
