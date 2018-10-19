package uk.co.seanhodges.incandescent.client.control

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.sdsmdg.harjot.crollerTest.Croller
import uk.co.seanhodges.incandescent.client.*
import uk.co.seanhodges.incandescent.client.auth.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.selection.DeviceEntity
import uk.co.seanhodges.incandescent.client.selection.RoomEntity
import java.lang.ref.WeakReference


class DeviceControlActivity : Activity(), DeviceChangeAware {

    private val server = Inject.server
    private val executor = Inject.executor
    private val deviceChangeHandler = Inject.deviceChangeHandler

    private lateinit var selectedRoom: RoomEntity
    private lateinit var selectedDevice: DeviceEntity

    @Volatile private var eventsPreventingUiChangeListeners = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

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
            setupDimmer()
        }
    }

    override fun onPostResume() {
        super.onPostResume()

        selectedDevice.powerCommand?.let { cmd -> executor.enqueueLoad(cmd) }
        selectedDevice.dimCommand?.let { cmd -> executor.enqueueLoad(cmd) }

        val authRepository = AuthRepository(WeakReference(applicationContext))
        if (!authRepository.isAuthenticated()) {
            startActivity(Intent(this, AuthenticateActivity::class.java))
        }
        else {
            executor.connectToServer(authRepository, onComplete = { success: Boolean ->
                if (success) {
                    Toast.makeText(this, "Connected to Lightwave server :)", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(this, "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
                }
            })
        }

        deviceChangeHandler.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        deviceChangeHandler.removeListener(this)
    }

    private fun setupDeviceInfo() {
        val roomInfo = findViewById<TextView>(R.id.room_info)
        roomInfo.text = selectedRoom.title
        val deviceInfo = findViewById<TextView>(R.id.device_info)
        deviceInfo.text = selectedDevice.title
    }

    private fun setupOnOffSwitches() {
        val offButton = findViewById<Button>(R.id.off_button)
        val onButton = findViewById<Button>(R.id.on_button)

        offButton.setOnClickListener {
            if (eventsPreventingUiChangeListeners > 0) return@setOnClickListener
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 0) }
//                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, 0) } // Workaround for a bug in my dimmer bulbs :)
            applyOnOffHighlight(false)
        }

        onButton.setOnClickListener {
            if (eventsPreventingUiChangeListeners > 0) return@setOnClickListener
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 1) }
//                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, 100) } // Workaround for a bug in my dimmer bulbs :)
            applyOnOffHighlight(true)
        }
    }

    private fun setupDimmer() {
        val croller = findViewById<View>(R.id.croller) as Croller
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
            if (eventsPreventingUiChangeListeners > 0) return@setOnProgressChangedListener
            selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, newValue) }
            selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, if (newValue > 0) 1 else 0) }
            applyOnOffHighlight(newValue > 0)
        };
    }

    override fun onDeviceChanged(featureId: String, newValue: Int) {
        Log.d(javaClass.name, "Device change detected: $featureId=$newValue")
        // FIXME(sean): for unsolicited changes the featureId is not returned in response,
        //              so we just assume group read is always for dimmer :/
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
    }

    private fun withUiChangeListenersDisabled(actions: () -> Unit) {
        runOnUiThread {
            ++eventsPreventingUiChangeListeners
            actions()
            // Account for the delayed callback behaviour in Croller
            Handler().postDelayed({ --eventsPreventingUiChangeListeners }, 1000)
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
