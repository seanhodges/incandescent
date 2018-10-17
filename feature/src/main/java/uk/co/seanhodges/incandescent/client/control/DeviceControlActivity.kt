package uk.co.seanhodges.incandescent.client.control

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.sdsmdg.harjot.crollerTest.Croller
import uk.co.seanhodges.incandescent.client.DeviceChangeAware
import uk.co.seanhodges.incandescent.client.DeviceChangeHandler
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.auth.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.selection.DeviceEntity
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class DeviceControlActivity : Activity(), DeviceChangeAware {

    private val server = LightwaveServer()
    private val executor = OperationExecutor(server)
    private val deviceChangeHandler = DeviceChangeHandler(server)

    private lateinit var selectedDevice: DeviceEntity

    private var disableListeners = true // FIXME(sean): hack because updating UI control values triggers a change

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        if (!intent.hasExtra("selectedDevice")) {
            Log.e(this.javaClass.name, "Device info missing when attempting to open DeviceControlActivity")
            finish()
            return
        }
        selectedDevice = intent.getSerializableExtra("selectedDevice") as DeviceEntity
        Log.d(this.javaClass.name, "Selected device is ${selectedDevice.id}")

        setupOnOffSwitches()
        setupDimmer()

        deviceChangeHandler.addListener(this)
    }

    override fun onPostResume() {
        super.onPostResume()

        disableListeners = true
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
    }

    override fun onPause() {
        super.onPause()

        server.disconnect()
    }

    private fun setupOnOffSwitches() {
        val offButton = findViewById<Button>(R.id.off_button)
        val onButton = findViewById<Button>(R.id.on_button)

        offButton.setOnClickListener {
            if (!disableListeners) {
                selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 0) }
//                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, 0) } // Workaround for a bug in my dimmer bulbs :)
                applyOnOffHighlight(false)
            }
        }

        onButton.setOnClickListener {
            if (!disableListeners) {
                selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, 1) }
//                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, 100) } // Workaround for a bug in my dimmer bulbs :)
                applyOnOffHighlight(true)
            }
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
            if (!disableListeners) {
                selectedDevice.dimCommand?.let { cmd -> executor.enqueueChange(cmd, newValue) }
                selectedDevice.powerCommand?.let { cmd -> executor.enqueueChange(cmd, if (newValue > 0) 1 else 0) }
                applyOnOffHighlight(newValue > 0)
            }
        };
    }

    override fun onDeviceChanged(featureId: String, newValue: Int) {
        Log.d(javaClass.name, "Device change detected: $featureId=$newValue")
        // FIXME(sean): since the featureId is not returned in response we assume group read is always for dimmer
        if (featureId.equals(selectedDevice.dimCommand) || featureId.equals("")) {
            runOnUiThread {
                disableListeners = true
                val croller = findViewById<View>(R.id.croller) as Croller
                croller.value = newValue
                applyOnOffHighlight(croller.value > 0)
                disableListeners = false
            }
        }
        else if (featureId.equals(selectedDevice.powerCommand)) {
            runOnUiThread {
                disableListeners = true
                applyOnOffHighlight(newValue == 1)
                disableListeners = false
            }
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
