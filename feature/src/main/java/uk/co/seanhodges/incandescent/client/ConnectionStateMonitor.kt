package uk.co.seanhodges.incandescent.client

import android.content.Context
import android.net.*
import android.util.Log
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import java.lang.ref.WeakReference

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

    fun resume() {
        Log.d(javaClass.name, "Started listening for network changes")
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    fun pause() {
        Log.d(javaClass.name, "Stopped listening for network changes")
        executor.stop()
        connectivityManager.unregisterNetworkCallback(this)
    }

    override fun onLost(network: Network?) {
        super.onLost(network)
        contextRef.get()?.let { context ->
            Log.d(javaClass.name, "Lost connection")
            executor.stop()
        }
        listenerRef.get()?.onConnectionLost()
    }

    override fun onUnavailable() {
        super.onUnavailable()
        contextRef.get()?.let { context ->
            Log.d(javaClass.name, "Connection unavailable")
            executor.stop()
        }
        listenerRef.get()?.onConnectionLost()
    }

    override fun onAvailable(network: Network?) {
        super.onAvailable(network)
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
