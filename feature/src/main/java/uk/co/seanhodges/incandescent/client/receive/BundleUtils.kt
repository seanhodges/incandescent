package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.twofortyfouram.assertion.BundleAssertions
import uk.co.seanhodges.incandescent.client.fragment.applianceList.FlatApplianceRow

private const val BUNDLE_EXTRA_ORIGIN = "uk.co.seanhodges.incandescent.client.receive.ORIGIN"
private const val BUNDLE_EXTRA_SCENES = "uk.co.seanhodges.incandescent.client.receive.SCENES"
private const val BUNDLE_EXTRA_APPLIANCES = "uk.co.seanhodges.incandescent.client.receive.APPLIANCES"

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

object BundleUtils {

    fun generateBundle(cmd : CommandBundle) = Bundle().apply {
        this.putString(BUNDLE_EXTRA_ORIGIN, cmd.origin)
        this.putStringArray(BUNDLE_EXTRA_SCENES, cmd.scenes?.toTypedArray() ?: emptyArray())
        this.putString(BUNDLE_EXTRA_APPLIANCES, packAppliances(cmd.appliances))
    }

    private fun packAppliances(appliances: List<ApplianceBundle>): String = Gson().toJson(appliances)
    private fun unpackAppliances(appliances: String?): List<ApplianceBundle> = Gson().fromJson(appliances ?: "[]")

    fun unpackBundle(bundle: Bundle): CommandBundle = CommandBundle(
            bundle.getString(BUNDLE_EXTRA_ORIGIN) ?: "",
            bundle.getStringArray(BUNDLE_EXTRA_SCENES)?.toList() ?: emptyList(),
            unpackAppliances(bundle.getString(BUNDLE_EXTRA_APPLIANCES))
    )

    fun isBundleValid(bundle: Bundle): Boolean {
        try {
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_ORIGIN, false, false)
        } catch (e: AssertionError) {
            Log.e(OperationReceiver::class.java.name, "Bundle failed verification%s", e)
            return false
        }
        return true
    }
}

data class CommandBundle(
        val origin: String,
        val scenes: List<String>,
        val appliances: List<ApplianceBundle>
)

data class ApplianceBundle(
        val id: String,
        val roomName: String,
        val applianceName: String,
        val power: Int,
        val dim: Int
) {
    constructor(data: FlatApplianceRow) : this(
        data.appliance.id,
        data.room.title,
        data.appliance.title,
        if (data.appliance.powerCommand != null) { data.appliance.lastPowerValue } else -1,
        if (data.appliance.dimCommand != null) { data.appliance.lastDimValue } else -1
    )
}