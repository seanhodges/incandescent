package uk.co.seanhodges.incandescent.client

import android.util.Log
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadFeature
import java.util.*
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer


class DeviceChangeHandler(server: LightwaveServer,
                          private var loadItemIdToFeatureId : MutableMap<Int, String> = mutableMapOf()
) : LWEventListener {

    private val listeners = ArrayList<DeviceChangeAware>()

    init {
        server.addListener(this)
    }

    fun addListener(listener: DeviceChangeAware) {
        listeners.add(listener)
    }

    fun removeListener(listener: DeviceChangeAware) {
        listeners.remove(listener)
    }

    @Suppress("SENSELESS_COMPARISON")
    override fun onEvent(event: LWEvent) {
        if (!(event.clazz == "feature" && (event.operation == "event" || event.operation == "read"))) {
            return // Filter only feature change events
        }

        if (event.items.size < 1 || event.items[0].payload == null) {
            Log.e(javaClass.name, "Could not process event due to missing payload: $event")
            return // No event payload, ignore this event
        }

        val payload : LWEventPayloadFeature = event.items[0].payload as LWEventPayloadFeature
        if (payload.featureId == null || payload.featureId.isEmpty()) {
            loadItemIdToFeatureId[event.items[0].itemId]?.let { itemId ->
                payload.featureId = itemId
            }
        }
        val featureId : String = payload.featureId

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