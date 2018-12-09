package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractLocalePluginActivity
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.DeviceDao
import uk.co.seanhodges.incandescent.client.storage.RoomDao

class MakeBundleActivity : AbstractLocalePluginActivity() {

    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()

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
        val roomName = findViewById<EditText>(R.id.room_name).text.toString()
        val deviceName = findViewById<EditText>(R.id.appliance_name).text.toString()

        val device = deviceDao.findByRoomAndDeviceName(roomName, deviceName)
        executor.enqueueLoadAll(arrayOf(device.powerCommand, device.dimCommand).filterNotNull())

        // FIXME: run executor first, then build bundle
        val operation = OperationBundle(roomName, deviceName, device.lastPowerValue == 1, device.lastDimValue)
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
            mIsCancelled = true // Signal to AbstractAppCompatPluginActivity that the user cancelled.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}