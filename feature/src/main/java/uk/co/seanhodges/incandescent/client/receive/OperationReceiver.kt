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

class OperationReceiver(
        private val executor: OperationExecutor = Inject.executor
) : AbstractPluginSettingReceiver() {

    override fun isBundleValid(bundle: Bundle): Boolean {
        return BundleUtils.isBundleValid(bundle)
    }

    override fun firePluginSetting(context: Context, bundle: Bundle) {
        Log.d(javaClass.name, "Intent received: $bundle")
        val operation = BundleUtils.unpackBundle(bundle)
        if (operation.roomName == null || operation.applianceName == null) return

        val deviceDao = AppDatabase.getDatabase(context).deviceDao()
        val device = deviceDao.findByRoomAndDeviceName(operation.roomName, operation.applianceName)
        applyPowerValue(operation, device)
        applyDimValue(operation, device)
    }

    private fun applyPowerValue(operation: OperationBundle, device: DeviceEntity) {
        operation.power?.let { value ->
            device.powerCommand?.let { cmd ->
                executor.enqueueChange(cmd, if (value) 1 else 0)
            }
        }
    }

    private fun applyDimValue(operation: OperationBundle, device: DeviceEntity) {
        operation.dim?.let { value ->
            device.dimCommand?.let { cmd ->
                executor.enqueueChange(cmd, value)
            }
        }
    }

    override fun isAsync(): Boolean {
        return false
    }
}