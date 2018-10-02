package uk.co.seanhodges.incandescent.client

import android.util.Log
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadFeature
import java.util.*
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer


class DeviceChangeHandler(server: LightwaveServer) : LWEventListener {

    private val listeners = ArrayList<DeviceChangeAware>()

    init {
        server.addListener(this)
    }

    fun addListener(listener: DeviceChangeAware) {
        listeners.add(listener)
    }

    override fun onEvent(event: LWEvent) {
        if (!(event.clazz.equals("feature") && (event.operation.equals("event") || event.operation.equals("read")))) {
            return // Filter only unsolicited feature change events
        }

        listeners.forEach {
            val payload : LWEventPayloadFeature = event.items[0].payload as LWEventPayloadFeature
            val featureId : String = payload.featureId ?: ""
            it.onDeviceChanged(featureId, payload.value)
        }
    }

    override fun onError(error: Throwable) {
        //TODO(sean): implement proper in-app error handling
        Log.e(javaClass.name, "Server error: " + error.message)
    }
}

interface DeviceChangeAware {
    fun onDeviceChanged(featureId: String, newValue: Int)
}