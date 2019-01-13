package uk.co.seanhodges.incandescent.client.receive

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver
import uk.co.seanhodges.incandescent.client.AuthenticationAware
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import java.lang.ref.WeakReference

private const val EXECUTOR_NAME : String = "Incandescent.Operation.Receiver"

class OperationReceiver(
        private val executor: OperationExecutor = Inject.executor
) : AbstractPluginSettingReceiver(), AuthenticationAware {

    private val handlerThread = HandlerThread(EXECUTOR_NAME)

    init {
        handlerThread.start()
    }

    override fun isBundleValid(bundle: Bundle): Boolean {
        return BundleUtils.isBundleValid(bundle)
    }

    override fun firePluginSetting(context: Context, bundle: Bundle) {
        Log.d(javaClass.name, "Intent received: $bundle")
        val cmd = BundleUtils.unpackBundle(bundle)
        if (cmd.scene.isNullOrEmpty() && cmd.appliances.isEmpty()) return

        val deviceDao = AppDatabase.getDatabase(context).deviceDao()
        val authRepository = AuthRepository(WeakReference(context))

        val handler = Handler(handlerThread.looper)
        handler.post {
            // TODO: Handle scenes

            val applianceIds = cmd.appliances.map { operation ->
                Log.d(javaClass.name, "Finding appliance ${operation.roomName} > ${operation.applianceName}")
                operation.id
            }
            val devices = deviceDao.findByIds(applianceIds)
            cmd.appliances.map { operation ->
                val device = devices.find { it.id == operation.id }
                if (device != null) {
                    applyPowerValue(operation, device)
                    applyDimValue(operation, device)
                }
            }
        }

        executor.start(authRepository, this)
    }

    override fun onAuthenticationFailed() {
        Log.e(javaClass.name, "Could not connect to Lightwave server :(")
    }

    private fun applyPowerValue(applicance: ApplicanceBundle, device: DeviceEntity) {
        applicance.power.let { value ->
            device.powerCommand?.let { cmd ->
                Log.d(javaClass.name, "Applying power $value")
                executor.enqueueChange(cmd, value)
            }
        }
    }

    private fun applyDimValue(applicance: ApplicanceBundle, device: DeviceEntity) {
        applicance.dim.let { value ->
            device.dimCommand?.let { cmd ->
                Log.d(javaClass.name, "Applying dim $value")
                executor.enqueueChange(cmd, value)
            }
        }
    }

    override fun isAsync(): Boolean {
        return false
    }
}