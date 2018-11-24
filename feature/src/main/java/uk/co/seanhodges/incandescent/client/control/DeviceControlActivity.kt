package uk.co.seanhodges.incandescent.client.control

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sdsmdg.harjot.crollerTest.Croller
import uk.co.seanhodges.incandescent.client.*
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import java.lang.ref.WeakReference
import androidx.core.app.NavUtils
import androidx.lifecycle.ViewModelProviders
import uk.co.seanhodges.incandescent.client.support.GatherDeviceReport
import uk.co.seanhodges.incandescent.client.support.ReportDeviceActivity


class DeviceControlActivity(
        private val executor: OperationExecutor = Inject.executor,
        private val deviceChangeHandler: DeviceChangeHandler = Inject.deviceChangeHandler
) : AppCompatActivity(), DeviceChangeAware {

    private lateinit var viewModel : DeviceControlViewModel
    private lateinit var selectedRoom: RoomEntity
    private lateinit var selectedDevice: DeviceEntity

    @Volatile private var eventsPreventingCrollerChangeListener = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        viewModel = ViewModelProviders.of(this).get(DeviceControlViewModel::class.java)

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

        selectedDevice.powerCommand?.let { cmd -> executor.enqueueLoad(cmd) }
        selectedDevice.dimCommand?.let { cmd -> executor.enqueueLoad(cmd) }

        val authRepository = AuthRepository(WeakReference(applicationContext))
        if (!authRepository.isAuthenticated()) {
            startActivity(Intent(this, AuthenticateActivity::class.java))
        }
        else {
            executor.connectToServer(authRepository, onComplete = { success: Boolean ->
                if (!success) {
                    Toast.makeText(this, "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, AuthenticateActivity::class.java))
                }
            })
        }

        deviceChangeHandler.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        deviceChangeHandler.removeListener(this)
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
//                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, 0) } // Workaround for a bug in my dimmer bulbs :)
            applyOnOffHighlight(false)
        }

        onButton.setOnClickListener {
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 1) }
//                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, 100) } // Workaround for a bug in my dimmer bulbs :)
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

    override fun onDeviceChanged(featureId: String, newValue: Int) {
        Log.d(javaClass.name, "Device change detected: $featureId=$newValue")
        if (featureId.equals(selectedDevice.dimCommand)) {
            withUiChangeListenersDisabled {
                val croller = findViewById<View>(R.id.croller) as Croller
                croller.value = newValue
            }
        }
        else if (featureId.equals(selectedDevice.powerCommand)) {
            withUiChangeListenersDisabled {
                applyOnOffHighlight(newValue == 1)
            }
        }

//        deviceDao.setLastValue()
//        viewModel.setLastValue(selectedDevice.id, )
    }

    private fun withUiChangeListenersDisabled(actions: () -> Unit) {
        runOnUiThread {
            ++eventsPreventingCrollerChangeListener
            actions()
            // Account for the delayed callback behaviour in Croller
            Handler().postDelayed({ --eventsPreventingCrollerChangeListener }, 1000)
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
