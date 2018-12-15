package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.Observer

import uk.co.seanhodges.incandescent.client.R
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractFragmentPluginActivity
import uk.co.seanhodges.incandescent.client.storage.*


class MakeBundleActivity : AbstractFragmentPluginActivity() {

    private val roomDao: RoomDao = AppDatabase.getDatabase(this).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(this).deviceDao()

    private val roomValues = mutableMapOf<String, RoomEntity>()
    private val deviceValues = mutableMapOf<String, DeviceEntity>()

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

        showRooms()
    }

    private fun showRooms() {
        roomDao.loadAll().observe(this, Observer<List<RoomEntity>> { rooms ->
            val spinner = this.findViewById<Spinner>(R.id.room_name)
            roomValues.clear()
            spinner.adapter = ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, rooms.map {
                roomValues[it.title] = it
                it.title
            })

            if (rooms.isNotEmpty()) {
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedItem = parent?.getItemAtPosition(position)
                        showDevicesForRoom(roomValues[selectedItem]?.id)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                showDevicesForRoom(rooms[0].id)
            }
        })
    }

    private fun showDevicesForRoom(roomId : String?) {
        if (roomId.isNullOrEmpty()) return
        val spinner = this.findViewById<Spinner>(R.id.appliance_name)
        deviceValues.clear()
        deviceDao.findByRoom(roomId!!).observe(this, Observer<List<DeviceEntity>> { devices ->
            spinner.adapter = ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, devices.map {
                deviceValues[it.title] = it
                it.title
            })
        })
    }

    override fun onPostCreateWithPreviousResult(previousBundle: Bundle, previousBlurb: String) {
        val operation = BundleUtils.unpackBundle(previousBundle)

        var spinner: Spinner = this.findViewById(R.id.room_name)
        (0..spinner.adapter.count).find {
            spinner.adapter.getItem(it).toString() == operation.roomName
        }?.let { spinner.setSelection(it) }

        spinner = this.findViewById(R.id.appliance_name)
        (0..spinner.adapter.count).find {
            spinner.adapter.getItem(it).toString() == operation.applianceName
        }?.let { spinner.setSelection(it) }
    }

    override fun isBundleValid(bundle: Bundle): Boolean {
        return BundleUtils.isBundleValid(bundle)
    }

    override fun getResultBundle(): Bundle? {
        var spinner: Spinner = this.findViewById(R.id.room_name)
        val roomName = spinner.selectedItem.toString()

        spinner = this.findViewById(R.id.appliance_name)
        val deviceName = spinner.selectedItem.toString()
        val device = deviceValues[deviceName]

        val operation = OperationBundle(roomName, deviceName, device?.lastPowerValue == 1, device?.lastDimValue)
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