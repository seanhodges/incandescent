package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import java.lang.ref.WeakReference

private const val PREFS_NAME : String = "Incandescent.Prefs.Settings"
private const val PREFS_VERSION : Int = 1

enum class DeviceListSize {
    SMALL,
    LARGE
}

class SettingsRepository(private val ctxRef: WeakReference<Context>) {

    fun updateDeviceListSize(deviceListSize: DeviceListSize) {
        ctxRef.get()?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                    .putInt("prefsVersion", PREFS_VERSION)
                    .putString("deviceListSize", deviceListSize.name)
                    .apply()
        }
    }

    fun updateShowOnlyActiveDevices(showOnlyActiveDevices: Boolean) {
        ctxRef.get()?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                    .putInt("prefsVersion", PREFS_VERSION)
                    .putBoolean("showOnlyActiveDevices", showOnlyActiveDevices)
                    .apply()
        }
    }

    fun get(): AppSettings {
        val prefs = ctxRef.get()!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceSizeList = prefs.getString("deviceListSize", DeviceListSize.SMALL.name) ?: DeviceListSize.SMALL.name
        val showOnlyActiveDevices = prefs.getBoolean("showOnlyActiveDevices", false)
        return AppSettings(DeviceListSize.valueOf(deviceSizeList), showOnlyActiveDevices)
    }
}

data class AppSettings(
        val deviceListSize: DeviceListSize,
        val showOnlyActiveDevices: Boolean
)