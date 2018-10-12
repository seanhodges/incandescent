package uk.co.seanhodges.incandescent.client

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.sdsmdg.harjot.crollerTest.Croller
import uk.co.seanhodges.incandescent.client.auth.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class DeviceControlActivity : Activity(), DeviceChangeAware {

    private val server = LightwaveServer()
    private val authRepository = AuthRepository(WeakReference(applicationContext))
    private val executor = OperationExecutor(server)
    private val deviceChangeHandler = DeviceChangeHandler(server)

    private var selectedRoom : String = "Living room"
    private var selectedSwitchFeature : String = FEATURE_LIVING_ROOM_SWITCH_ID
    private var selectedDimFeature : String = FEATURE_LIVING_ROOM_DIM_ID

    private var disableListeners = true // FIXME(sean): hack because updating UI control values triggers a change

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        setupLocationSelectors()
        setupOnOffSwitches()
        setupDimmer()

        deviceChangeHandler.addListener(this)
    }

    override fun onPostResume() {
        super.onPostResume()

        disableListeners = true
        executor.enqueueLoad(selectedSwitchFeature)
        executor.enqueueLoad(selectedDimFeature)

        if (authRepository.isAuthenticated()) {
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

    private fun setupLocationSelectors() {
        val roomButton = findViewById<Button>(R.id.room_button)
        roomButton.setOnClickListener { btn: View ->
            //TODO(sean): locations are hardcoded for now...
            if (selectedRoom.equals("Living room")) {
                selectedRoom = "Bedroom"
                selectedSwitchFeature = FEATURE_BEDROOM_SWITCH_ID
                selectedDimFeature = FEATURE_BEDROOM_DIM_ID
            }
            else {
                selectedRoom = "Living room"
                selectedSwitchFeature = FEATURE_LIVING_ROOM_SWITCH_ID
                selectedDimFeature = FEATURE_LIVING_ROOM_DIM_ID
            }
            (btn as Button).text = selectedRoom
            executor.enqueueLoad(selectedSwitchFeature)
            executor.enqueueLoad(selectedDimFeature)
        }
    }

    private fun setupOnOffSwitches() {
        val offButton = findViewById<Button>(R.id.off_button)
        val onButton = findViewById<Button>(R.id.on_button)

        offButton.setOnClickListener {
            if (!disableListeners) {
                executor.enqueueChange(selectedSwitchFeature, 0)
                executor.enqueueChange(selectedDimFeature, 0) // Workaround for a bug in my dimmer bulbs :)
                applyOnOffHighlight(false)
            }
        }

        onButton.setOnClickListener {
            if (!disableListeners) {
                executor.enqueueChange(selectedSwitchFeature, 1)
                executor.enqueueChange(selectedDimFeature, 100) // Workaround for a bug in my dimmer bulbs :)
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
                executor.enqueueChange(selectedDimFeature, newValue)
                executor.enqueueChange(selectedSwitchFeature, if (newValue > 0) 1 else 0)
                applyOnOffHighlight(newValue > 0)
            }
        };
    }

    override fun onDeviceChanged(featureId: String, newValue: Int) {
        Log.d(javaClass.name, "Device change detected: $featureId=$newValue")
        // FIXME(sean): since the featureId is not returned in response we assume group read is always for dimmer
        if (featureId.equals(selectedDimFeature) || featureId.equals("")) {
            runOnUiThread {
                disableListeners = true
                val croller = findViewById<View>(R.id.croller) as Croller
                croller.progress = newValue
                applyOnOffHighlight(croller.progress > 0)
                disableListeners = false
            }
        }
        else if (featureId.equals(selectedSwitchFeature)) {
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

    companion object {
        private val FEATURE_LIVING_ROOM_SWITCH_ID = "5b8aa9b4d36c330fd5b4e100-22-3157332334+1"
        private val FEATURE_LIVING_ROOM_DIM_ID = "5b8aa9b4d36c330fd5b4e100-23-3157332334+1"

        private val FEATURE_BEDROOM_SWITCH_ID = "5b8aa9b4d36c330fd5b4e100-46-3157332334+1"
        private val FEATURE_BEDROOM_DIM_ID = "5b8aa9b4d36c330fd5b4e100-47-3157332334+1"
    }
}
