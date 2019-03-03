package uk.co.seanhodges.incandescent.client.selection

import android.app.Activity
import android.graphics.RectF
import android.util.Log
import com.robinhood.spark.SparkAdapter
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.fixedRateTimer

class EnergyMonitor(
        private val device: DeviceEntity,
        private val adapter : RollingSparkAdapter,
        private var listener: EnergyAware? = null,
        private val server: LightwaveServer = Inject.server,
        private val executor: OperationExecutor = Inject.executor
) {

    fun start() {
        val powerFeatureId = device.powerUsageCommand // Current watts
        val energyFeatureId = device.energyConsumptionCommand // kWh (cumulative)
        if (powerFeatureId == null || energyFeatureId == null) return

        // Listen for energy value changes
        EnergyChangeHandler(server, adapter, listener, powerFeatureId, energyFeatureId)

        // Poll latest usage
        fixedRateTimer("", false, 0, 3000) {
            executor.enqueueLoadAll(listOf(powerFeatureId, energyFeatureId))
        }
    }
}

class EnergyChangeHandler(
        server: LightwaveServer,
        private val adapter: RollingSparkAdapter,
        private var listener: EnergyAware?,
        private val powerFeatureId: String,
        private val energyFeatureId: String
) : LWEventListener {

    private var lastPowerValue: Int = 0
    private var lastEnergyValue: Float = 0.0f

    init {
        server.addListener(this)
    }

    override fun onEvent(event: LWEvent) {
        Log.d(javaClass.name, "Received event")
        if (!(event.clazz == "feature" && (event.operation == "event" || event.operation == "read"))) {
            return // Filter only feature change events
        }

        if (event.items.size < 1) {
            Log.e(javaClass.name, "Could not process event due to missing payload: $event")
            return // No event payload, ignore this event
        }

        val payload : LWEventPayloadFeature = event.items[0].payload as LWEventPayloadFeature

        if (payload.featureId == powerFeatureId) {
            Log.d(javaClass.name, "Watts: ${payload.value}")
            lastPowerValue = payload.value
            adapter.addMetric(payload.value.toFloat())
            listener?.onEnergyChanged(lastPowerValue, lastEnergyValue)
        }
        else if (payload.featureId == energyFeatureId) {
            Log.d(javaClass.name, "kWh: ${payload.value}")
            lastEnergyValue = if (payload.value == 0) 0.0f else payload.value / 1000.0f
            listener?.onEnergyChanged(lastPowerValue, lastEnergyValue)
        }

    }

    override fun onError(error: Throwable) {
        Log.e(javaClass.name, "Server error: " + error.message)
    }
}

interface EnergyAware {
    fun onEnergyChanged(wattsValue: Int, kwhValue: Float)
}

class RollingSparkAdapter(
        ctx: Activity,
        maxHistory: Int = 20,
        private val minScaleY: Int = 200
) : SparkAdapter() {

    private val yData: FloatArray = FloatArray(maxHistory)
    private val ctxRef = WeakReference(ctx)
    private var baseLine: Float? = null

    override fun getDataBounds(): RectF {
        val count = count
        val hasBaseLine = hasBaseLine()

        var minY = if (hasBaseLine) getBaseLine() - minScaleY else java.lang.Float.MAX_VALUE
        var maxY = if (hasBaseLine) getBaseLine() + minScaleY else -java.lang.Float.MAX_VALUE
        var minX = java.lang.Float.MAX_VALUE
        var maxX = -java.lang.Float.MAX_VALUE

        for (i in 0 until count) {
            val x = getX(i)
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)

            val y = getY(i)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
        }

        return RectF(minX, minY, maxX, maxY)
    }

    fun addMetric(value: Float) {
        if (baseLine == null) {
            baseLine = value
            Arrays.fill(yData, value)
        }
        else {
            System.arraycopy(yData, 1, yData, 0, yData.size - 1)
            yData[yData.size - 1] = value
        }

        ctxRef.get().apply {
            this?.runOnUiThread {
                notifyDataSetChanged()
            }
        }
    }

    fun reset() {
        baseLine = yData[yData.size - 1]
        Arrays.fill(yData, yData[yData.size - 1])
    }

    override fun hasBaseLine(): Boolean {
        return baseLine != null
    }

    override fun getBaseLine(): Float {
        return baseLine ?: 0.0f
    }

    override fun getY(index: Int): Float {
        return yData[index]
    }

    override fun getItem(index: Int): Any {
        return yData[index]
    }

    override fun getCount(): Int {
        return yData.size
    }

}