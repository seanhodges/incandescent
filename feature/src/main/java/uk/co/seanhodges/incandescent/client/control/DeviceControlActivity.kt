package uk.co.seanhodges.incandescent.client.control

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sdsmdg.harjot.crollerTest.Croller
import uk.co.seanhodges.incandescent.client.*
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import java.lang.ref.WeakReference
import androidx.core.app.NavUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import uk.co.seanhodges.incandescent.client.support.GatherDeviceReport
import uk.co.seanhodges.incandescent.client.support.ReportDeviceActivity


class DeviceControlActivity(
        private val launch: LaunchActivity = Inject.launch,
        private val executor: OperationExecutor = Inject.executor
) : AppCompatActivity() {

    private lateinit var viewModel : DeviceControlViewModel
    private lateinit var selectedRoom: RoomEntity
    private lateinit var selectedDevice: DeviceEntity

    @Volatile private var eventsPreventingCrollerChangeListener = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        viewModel = ViewModelProviders.of(this).get(DeviceControlViewModel::class.java)
        viewModel.listenForValueChanges(this)

        if (!intent.hasExtra("selectedRoom")) {
            Log.e(this.javaClass.name, "Room info missing when attempting to open DeviceControlActivity")
            finish()
            return
        }
        selectedRoom = intent.getSerializableExtra("selectedRoom") as RoomEntity
        if (!intent.hasExtra("selectedDevice")) {
            Log.e(this.javaClass.name, "Device info missing when attempting to open DeviceControlActivity")
            finish()
            return
        }
        selectedDevice = intent.getSerializableExtra("selectedDevice") as DeviceEntity
        Log.d(this.javaClass.name, "Selected device is ${selectedDevice.id}")

        withUiChangeListenersDisabled {
            setupDeviceInfo()
            setupOnOffSwitches()

            if (selectedDevice.type == "light") {
                setupDimmer()
            }
            else {
                hideDimmer()
            }
        }

        incrementPopularityCounters()

        executor.reportHandler = { packet ->
            //@see OperationExecutor.onRawEvent()
            GatherDeviceReport(this).saveReport(packet)
        }

        viewModel.getDevice(selectedDevice.id).observe(this, Observer<DeviceEntity> { device ->
            onDeviceChanged(device)
        })
        onDeviceChanged(selectedDevice)
    }

    private fun setupActionBar() {
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun incrementPopularityCounters() {
        viewModel.incChosenCount(selectedRoom.id, selectedDevice.id)
    }

    override fun onResume() {
        super.onResume()
        if (isNetworkDown()) {
            val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            builder.setTitle(resources.getString(R.string.alert_title_no_internet))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(resources.getString(R.string.alert_message_no_internet))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        finish()
                        startActivity(intent)
                    }
                    .show()
            return
        }

        eventsPreventingCrollerChangeListener = 0

        withUiChangeListenersDisabled {
            Log.d(javaClass.name, "Setting initial device values")
            val croller = findViewById<View>(R.id.croller) as Croller
            croller.value = selectedDevice.lastDimValue
            applyOnOffHighlight(selectedDevice.lastPowerValue == 1)
        }
        selectedDevice.powerCommand?.let { cmd -> executor.enqueueLoad(cmd) }
        selectedDevice.dimCommand?.let { cmd -> executor.enqueueLoad(cmd) }
        viewModel.listenForValueChanges(this)

        val authRepository = AuthRepository(WeakReference(applicationContext))
        if (!authRepository.isAuthenticated()) {
            launch.authenticate(this)
        }
        else {
            executor.connectToServer(authRepository, onComplete = { success: Boolean ->
                if (!success) {
                    Toast.makeText(this, "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
                    launch.authenticate(this)
                }
            })
        }
    }

    private fun isNetworkDown(): Boolean {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting != true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putSerializable("selectedRoom", selectedRoom)
        outState?.putSerializable("selectedDevice", selectedDevice)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_device_control, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.menu_item_report_device -> {
                val intent = Intent(this, ReportDeviceActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDeviceInfo() {
        val roomDrawable = IconResolver.getRoomImage(selectedRoom.title)
        val roomImage = findViewById<ImageView>(R.id.room_image)
        roomImage.setImageResource(roomDrawable)

        val deviceDrawable = IconResolver.getDeviceImage(selectedDevice.title, selectedDevice.type)
        val deviceImage = findViewById<ImageView>(R.id.device_image)
        deviceImage.setImageResource(deviceDrawable)

        supportActionBar?.setIcon(deviceDrawable)
        supportActionBar?.title = "${selectedRoom.title} > ${selectedDevice.title}"
    }

    private fun setupOnOffSwitches() {
        val offButton = findViewById<Button>(R.id.off_button)
        val onButton = findViewById<Button>(R.id.on_button)

        offButton.setOnClickListener {
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 0) }
            applyOnOffHighlight(false)
        }

        onButton.setOnClickListener {
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 1) }
            applyOnOffHighlight(true)
        }
    }

    private fun hideDimmer() {
        val croller = findViewById<View>(R.id.croller) as Croller
        croller.visibility = View.GONE
    }

    private fun setupDimmer() {
        val croller = findViewById<View>(R.id.croller) as Croller
        croller.visibility = View.VISIBLE
        croller.indicatorWidth = 10f
        croller.backCircleColor = Color.parseColor("#EDEDED")
        croller.mainCircleColor = Color.WHITE
        croller.min = 0
        croller.max = 100
        croller.startOffset = 45
        croller.setIsContinuous(false)
        croller.labelColor = Color.BLACK
        croller.progressPrimaryColor = Color.parseColor("#0B3C49")
        croller.indicatorColor = Color.parseColor("#0B3C49")
        croller.progressSecondaryColor = Color.parseColor("#EEEEEE")
        croller.setOnProgressChangedListener { newValue ->
            if (eventsPreventingCrollerChangeListener > 0) return@setOnProgressChangedListener
            selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, newValue) }
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, if (newValue > 0) 1 else 0) }
            applyOnOffHighlight(newValue > 0)
        };
    }

    private fun onDeviceChanged(device: DeviceEntity) {
        if (selectedDevice.dimCommand != null && selectedDevice.lastDimValue != device.lastDimValue) {
            selectedDevice.lastDimValue = device.lastDimValue
            withUiChangeListenersDisabled {
                val croller = findViewById<View>(R.id.croller) as Croller
                croller.value = device.lastDimValue
            }
        }

        if (selectedDevice.powerCommand != null && selectedDevice.lastPowerValue != device.lastPowerValue) {
            selectedDevice.lastPowerValue = device.lastPowerValue
            withUiChangeListenersDisabled {
                applyOnOffHighlight(device.lastPowerValue == 1)
            }
        }
    }

    private fun withUiChangeListenersDisabled(actions: () -> Unit) {
        runOnUiThread {
            ++eventsPreventingCrollerChangeListener
            actions()
            // Account for the delayed callback behaviour in Croller
            Handler().postDelayed({
                if (--eventsPreventingCrollerChangeListener < 0) {
                    eventsPreventingCrollerChangeListener = 0
                }
            }, 1000)
        }
    }

    private fun applyOnOffHighlight(on: Boolean) {
        val offButton = findViewById<Button>(R.id.off_button)
        val onButton = findViewById<Button>(R.id.on_button)

        if (on) {
            offButton.background.clearColorFilter()
            onButton.background.setColorFilter(this.getColor(R.color.btn_on_active), PorterDuff.Mode.MULTIPLY)
        }
        else {
            offButton.background.setColorFilter(this.getColor(R.color.btn_off_active), PorterDuff.Mode.MULTIPLY)
            onButton.background.clearColorFilter()
        }
    }
}
