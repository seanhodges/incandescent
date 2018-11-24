package uk.co.seanhodges.incandescent.client

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import uk.co.seanhodges.incandescent.client.storage.DeviceDao
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadFeature

class LastValueChangeListener(
        private var loadItemIdToFeatureId : MutableMap<Int, String> = mutableMapOf()
) : LWEventListener {

    private var owner : LifecycleOwner? = null
    private var deviceDao : DeviceDao? = null

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
            payload.featureId = loadItemIdToFeatureId[event.items[0].itemId]!!
        }
        val featureId : String = payload.featureId

        deviceDao?.findByCommandId(featureId)?.observe(owner!!, Observer<DeviceEntity> { device ->
            if (featureId == device.dimCommand) {
                deviceDao?.setLastDimValue(device.id, payload.value)
            }
            else if (featureId == device.powerCommand) {
                deviceDao?.setLastPowerValue(device.id, payload.value)
            }
        })
    }

    override fun onError(error: Throwable) {}

}