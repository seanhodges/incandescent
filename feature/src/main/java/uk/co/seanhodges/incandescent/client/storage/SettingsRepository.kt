package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import java.lang.ref.WeakReference

private const val PREFS_NAME : String = "Incandescent.Prefs.Settings"
private const val PREFS_VERSION : Int = 1

enum class DeviceViewMode {
    GRID,
    LIST
}

class SettingsRepository(private val ctxRef: WeakReference<Context>) {

    fun updateDeviceViewMode(deviceViewMode: DeviceViewMode) {
        ctxRef.get()?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                    .putInt("prefsVersion", PREFS_VERSION)
                    .putString("deviceViewMode", deviceViewMode.name)
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
        val deviceViewMode = prefs.getString("deviceViewMode", DeviceViewMode.GRID.name) ?: DeviceViewMode.GRID.name
        val showOnlyActiveDevices = prefs.getBoolean("showOnlyActiveDevices", false)
        return AppSettings(DeviceViewMode.valueOf(deviceViewMode), showOnlyActiveDevices)
    }
}

data class AppSettings(
        val deviceViewMode: DeviceViewMode,
        val showOnlyActiveDevices: Boolean
)