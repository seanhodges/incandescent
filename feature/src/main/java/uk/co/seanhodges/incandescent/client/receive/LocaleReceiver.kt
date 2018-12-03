package uk.co.seanhodges.incandescent.client.receive

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.twofortyfouram.assertion.BundleAssertions
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity

private const val BUNDLE_EXTRA_ROOM_NAME = "uk.co.seanhodges.incandescent.client.receive.ROOM_NAME"
private const val BUNDLE_EXTRA_APPLIANCE_NAME = "uk.co.seanhodges.incandescent.client.receive.APPLIANCE_NAME"
private const val BUNDLE_EXTRA_VALUE_POWER = "uk.co.seanhodges.incandescent.client.receive.VALUE_POWER"
private const val BUNDLE_EXTRA_VALUE_DIM = "uk.co.seanhodges.incandescent.client.receive.VALUE_DIM"

fun generateBundle(context: Context, room: String, device: String, power: Boolean?, dim: Int?): Bundle {
    val result = Bundle();
    result.putString(BUNDLE_EXTRA_ROOM_NAME, room);
    result.putString(BUNDLE_EXTRA_APPLIANCE_NAME, device);
    power?.let {
        result.putBoolean(BUNDLE_EXTRA_VALUE_POWER, it)
    }
    dim?.let {
        result.putInt(BUNDLE_EXTRA_VALUE_POWER, it)
    }
    return result;
}

class LocaleReceiver(
        private val executor: OperationExecutor = Inject.executor
) : AbstractPluginSettingReceiver() {

    override fun isBundleValid(bundle: Bundle): Boolean {
        try {
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_ROOM_NAME, false, false);
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_APPLIANCE_NAME, false, false);
        } catch (e: AssertionError) {
            Log.e(LocaleReceiver::class.java.name, "Bundle failed verification%s", e);
            return false;
        }

        return true;
    }

    override fun firePluginSetting(context: Context, bundle: Bundle) {
        Log.d(javaClass.name, "Intent received: $bundle")
        val roomName = bundle.getString(BUNDLE_EXTRA_ROOM_NAME)
        val deviceName = bundle.getString(BUNDLE_EXTRA_APPLIANCE_NAME)
        if (roomName == null || deviceName == null) return

        val deviceDao = AppDatabase.getDatabase(context).deviceDao()
        val device = deviceDao.findByRoomAndDeviceName(roomName, deviceName)
        applyPowerValue(bundle, device)
        applyDimValue(bundle, device)
    }

    private fun applyPowerValue(bundle: Bundle, device: DeviceEntity) {
        if (bundle.containsKey(BUNDLE_EXTRA_VALUE_POWER)) {
            bundle.getBoolean(BUNDLE_EXTRA_VALUE_POWER).let { value ->
                device.powerCommand?.let { cmd ->
                    executor.enqueueChange(cmd, if (value) 1 else 0)
                }
            }
        }
    }

    private fun applyDimValue(bundle: Bundle, device: DeviceEntity) {
        if (bundle.containsKey(BUNDLE_EXTRA_VALUE_DIM)) {
            bundle.getInt(BUNDLE_EXTRA_VALUE_DIM).let { value ->
                device.dimCommand?.let { cmd ->
                    executor.enqueueChange(cmd, value)
                }
            }
        }
    }

    override fun isAsync(): Boolean {
        return false;
    }
}
