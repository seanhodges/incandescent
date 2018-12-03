package uk.co.seanhodges.incandescent.client.receive

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.twofortyfouram.assertion.BundleAssertions

private const val BUNDLE_EXTRA_ROOM_NAME = "uk.co.seanhodges.incandescent.client.receive.ROOM_NAME"
private const val BUNDLE_EXTRA_APPLIANCE_NAME = "uk.co.seanhodges.incandescent.client.receive.APPLIANCE_NAME"
private const val BUNDLE_EXTRA_VALUE_POWER = "uk.co.seanhodges.incandescent.client.receive.VALUE_POWER"
private const val BUNDLE_EXTRA_VALUE_DIM = "uk.co.seanhodges.incandescent.client.receive.VALUE_DIM"

object BundleUtils {

    fun generateBundle(operation: OperationBundle) = Bundle().apply {
        this.putString(BUNDLE_EXTRA_ROOM_NAME, operation.roomName)
        this.putString(BUNDLE_EXTRA_APPLIANCE_NAME, operation.applianceName)
        operation.power?.let {
            this.putBoolean(BUNDLE_EXTRA_VALUE_POWER, it)
        }
        operation.dim?.let {
            this.putInt(BUNDLE_EXTRA_VALUE_DIM, it)
        }
    }

    fun unpackBundle(bundle: Bundle): OperationBundle = OperationBundle(
        bundle.getString(BUNDLE_EXTRA_ROOM_NAME),
        bundle.getString(BUNDLE_EXTRA_APPLIANCE_NAME),
        if (bundle.containsKey(BUNDLE_EXTRA_VALUE_POWER)) bundle.getBoolean(BUNDLE_EXTRA_VALUE_POWER) else null,
        if (bundle.containsKey(BUNDLE_EXTRA_VALUE_DIM)) bundle.getInt(BUNDLE_EXTRA_VALUE_DIM) else null
    )

    fun isBundleValid(bundle: Bundle): Boolean {
        try {
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_ROOM_NAME, false, false)
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_APPLIANCE_NAME, false, false)
        } catch (e: AssertionError) {
            Log.e(OperationReceiver::class.java.name, "Bundle failed verification%s", e)
            return false
        }
        return true
    }
}

data class OperationBundle(
        val roomName: String?,
        val applianceName: String?,
        val power: Boolean?,
        val dim: Int?
)