package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.lifecycle.Observer

import uk.co.seanhodges.incandescent.client.R
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractFragmentPluginActivity
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LaunchActivity
import uk.co.seanhodges.incandescent.client.storage.*


class MakeBundleActivity(
        private val launch: LaunchActivity = Inject.launch
) : AbstractFragmentPluginActivity() {

    private lateinit var roomDao: RoomDao
    private lateinit var deviceDao: DeviceDao

    private val roomValues = mutableMapOf<String, RoomEntity>()
    private val deviceValues = mutableMapOf<String, DeviceEntity>()

    private var selectedRoom: RoomEntity? = null
    private var selectedDevice: DeviceEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_bundle)

        roomDao = AppDatabase.getDatabase(this).roomDao()
        deviceDao = AppDatabase.getDatabase(this).deviceDao()

        try {
            title = packageManager.getApplicationLabel(packageManager.getApplicationInfo(callingPackage, 0))
        } catch (e: Exception) {
            Log.e(javaClass.name, "Calling package couldn't be found", e)
        }

        actionBar?.setSubtitle(R.string.application_name)
        actionBar?.setDisplayHomeAsUpEnabled(true)


        val controlButton = this.findViewById<ImageButton>(R.id.control_button)
        controlButton.setOnClickListener {
            if (selectedRoom != null && selectedDevice != null) {
                launch.deviceControl(this, selectedRoom, selectedDevice)
            }
        }

        showRooms()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putSerializable("selectedRoom", selectedRoom)
        outState?.putSerializable("selectedDevice", selectedDevice)

        var spinner: Spinner = this.findViewById(R.id.room_name)
        outState?.putSerializable("selectedRoomId", spinner.selectedItemPosition)
        spinner = this.findViewById(R.id.appliance_name)
        outState?.putSerializable("selectedDeviceId", spinner.selectedItemPosition)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            selectedRoom = savedInstanceState.get("selectedRoom") as RoomEntity
            selectedDevice = savedInstanceState.get("selectedDevice") as DeviceEntity

            var spinner: Spinner = this.findViewById(R.id.room_name)
            spinner.setSelection(savedInstanceState.get("selectedRoomId") as Int)
            spinner = this.findViewById(R.id.appliance_name)
            spinner.setSelection(savedInstanceState.get("selectedDeviceId") as Int)
            showChosenDeviceInfo()
        }
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
                        selectedRoom = roomValues[selectedItem]
                        showDevicesForChosenRoom()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                selectedRoom = rooms[0]
                showDevicesForChosenRoom()
            }
        })
    }

    private fun showDevicesForChosenRoom() {
        if (selectedRoom?.id.isNullOrEmpty()) return
        val spinner = this.findViewById<Spinner>(R.id.appliance_name)
        deviceValues.clear()
        deviceDao.findByRoom(selectedRoom?.id!!).observe(this, Observer<List<DeviceEntity>> { devices ->
            spinner.adapter = ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, devices.map {
                deviceValues[it.title] = it
                it.title
            })
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedItem = parent?.getItemAtPosition(position)
                    selectedDevice = deviceValues[selectedItem]
                    showChosenDeviceInfo()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            selectedDevice = devices[0]
            showChosenDeviceInfo()
        })
    }

    private fun showChosenDeviceInfo() {
        val deviceInfo = this.findViewById<TextView>(R.id.device_info)
        deviceInfo.text = buildLabel(selectedRoom, selectedDevice)
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

    private fun buildLabel(roomEntity: RoomEntity?, deviceEntity: DeviceEntity?): String =
            buildLabel(roomEntity?.title, deviceEntity?.title, deviceEntity?.lastPowerValue == 1, deviceEntity?.lastDimValue)

    private fun buildLabel(operation: OperationBundle): String =
            buildLabel(operation.roomName, operation.applianceName, operation.power, operation.dim)

    private fun buildLabel(roomName: String?, applianceName: String?, power: Boolean?, dim: Int?): String {
        val status = when (power) {
            true -> if (dim ?: 0 > 0) "On, ${dim}%" else "On"
            else -> "Off"
        }
        return "${roomName} > ${applianceName} ($status)"
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