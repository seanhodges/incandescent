package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.twofortyfouram.assertion.BundleAssertions

private const val BUNDLE_EXTRA_SCENE = "uk.co.seanhodges.incandescent.client.receive.SCENE"
private const val BUNDLE_EXTRA_APPLIANCES = "uk.co.seanhodges.incandescent.client.receive.APPLIANCES"

object BundleUtils {

    fun generateBundle(cmd : CommandBundle) = Bundle().apply {
        cmd.scene?.let {
            this.putString(BUNDLE_EXTRA_SCENE, it)
        }
        cmd.appliances?.let {
            this.putParcelableArrayList(BUNDLE_EXTRA_APPLIANCES, ArrayList(it))
        }
    }

    fun unpackBundle(bundle: Bundle): CommandBundle = CommandBundle(
        bundle.getString(BUNDLE_EXTRA_SCENE),
        bundle.getParcelableArrayList(BUNDLE_EXTRA_APPLIANCES)
    )

    fun isBundleValid(bundle: Bundle): Boolean {
        try {
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_SCENE, true, false)
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_APPLIANCES, true, false)
        } catch (e: AssertionError) {
            Log.e(OperationReceiver::class.java.name, "Bundle failed verification%s", e)
            return false
        }
        return true
    }
}

data class CommandBundle(
        val scene: String?,
        val appliances: List<OperationBundle>?
)

data class OperationBundle(
        val roomName: String,
        val applianceName: String,
        val power: Boolean?,
        val dim: Int?
): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
            parcel.readValue(Int::class.java.classLoader) as? Int)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(roomName)
        parcel.writeString(applianceName)
        parcel.writeValue(power)
        parcel.writeValue(dim)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OperationBundle> {
        override fun createFromParcel(parcel: Parcel): OperationBundle {
            return OperationBundle(parcel)
        }

        override fun newArray(size: Int): Array<OperationBundle?> {
            return arrayOfNulls(size)
        }
    }
}