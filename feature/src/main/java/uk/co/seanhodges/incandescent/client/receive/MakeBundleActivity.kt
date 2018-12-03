package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractLocalePluginActivity
import uk.co.seanhodges.incandescent.client.R

class MakeBundleActivity : AbstractLocalePluginActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_make_bundle)

        try {
            title = packageManager.getApplicationLabel(packageManager.getApplicationInfo(callingPackage, 0))
        } catch (e: Exception) {
            Log.e(javaClass.name, "Calling package couldn't be found", e)
        }

        actionBar?.setSubtitle(R.string.application_name)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPostCreateWithPreviousResult(previousBundle: Bundle, previousBlurb: String) {
        val operation = BundleUtils.unpackBundle(previousBundle)
        this.findViewById<EditText>(R.id.room_name).setText(operation.roomName)
        this.findViewById<EditText>(R.id.appliance_name).setText(operation.applianceName)
    }

    override fun isBundleValid(bundle: Bundle): Boolean {
        return BundleUtils.isBundleValid(bundle)
    }

    override fun getResultBundle(): Bundle? {
        val operation = OperationBundle(
                findViewById<EditText>(R.id.room_name).text.toString(),
                findViewById<EditText>(R.id.appliance_name).text.toString(),
                null, null // TODO
        )
        if (!TextUtils.isEmpty(operation.roomName) && !TextUtils.isEmpty(operation.applianceName)) {
            return BundleUtils.generateBundle(operation)
        }
        return null
    }

    override fun getResultBlurb(bundle: Bundle): String {
        val operation = BundleUtils.unpackBundle(bundle)
        val blurb = buildLabel(operation)

        val maxBlurbLength = resources.getInteger(
                R.integer.com_twofortyfouram_locale_sdk_client_maximum_blurb_length)
        if (blurb.length > maxBlurbLength) {
            // Truncate the start of the blurb
            return "..." + blurb.substring(blurb.length - maxBlurbLength - 4, blurb.length - 1)
        }
        return blurb
    }

    private fun buildLabel(operation: OperationBundle): String {
        val status = when (operation.power) {
            true -> if (operation.dim ?: 0 > 0) "On, ${operation.dim}%" else "On"
            else -> "Off"
        }
        return "${operation.roomName} > ${operation.applianceName} ($status)"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_make_bundle, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (android.R.id.home == item.itemId) {
            finish()
        }
        else if (R.id.menu_cancel == item.itemId) {
            mIsCancelled = true // Signal to AbstractAppCompatPluginActivity that the user canceled.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}