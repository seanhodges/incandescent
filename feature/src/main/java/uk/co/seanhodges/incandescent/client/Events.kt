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

        if (event.items.size < 1 || event.items[0].payload == null) {
            Log.e(javaClass.name, "Could not process event due to missing payload: $event")
            return // No event payload, ignore this event
        }

        val payload : LWEventPayloadFeature = event.items[0].payload as LWEventPayloadFeature
        val featureId : String = payload.featureId ?: ""

        listeners.forEach {
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