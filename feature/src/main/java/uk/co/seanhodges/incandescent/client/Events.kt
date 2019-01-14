package uk.co.seanhodges.incandescent.client

import android.util.Log
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import androidx.lifecycle.LifecycleOwner
import uk.co.seanhodges.incandescent.client.storage.DeviceDao

class DeviceChangeHandler(
        server: LightwaveServer,
        private var loadItemIdToFeatureId : MutableMap<Int, String> = mutableMapOf()
) : LWEventListener {

    private val listeners = ArrayList<DeviceChangeAware>()
    private var owner : LifecycleOwner? = null
    private var deviceDao : DeviceDao? = null

    init {
        server.addListener(this)
    }

    fun addListener(listener: DeviceChangeAware) {
        listeners.add(listener)
    }

    fun removeListener(listener: DeviceChangeAware) {
        listeners.remove(listener)
    }

    fun setRepository(owner : LifecycleOwner, deviceDao : DeviceDao) {
        this.owner = owner
        this.deviceDao = deviceDao
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

        Log.d(javaClass.name, "Finding appliance by featureId $featureId")
        val device = deviceDao?.findByCommandId(featureId)
        if (device == null) {
            Log.w(javaClass.name, "No appliance found")
            return
        }
        Log.d(javaClass.name, "Device found: ${device.id}")
        if (featureId == device.dimCommand) {
            Log.d(javaClass.name, "Updating dim value to ${payload.value}")
            deviceDao?.setLastDimValue(device.id, payload.value)
        } else if (featureId == device.powerCommand) {
            Log.d(javaClass.name, "Updating power value to ${payload.value}")
            deviceDao?.setLastPowerValue(device.id, payload.value)
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