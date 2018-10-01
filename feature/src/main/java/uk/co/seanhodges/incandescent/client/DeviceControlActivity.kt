package uk.co.seanhodges.incandescent.client

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.sdsmdg.harjot.crollerTest.Croller
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class DeviceControlActivity : Activity(), DeviceChangeAware {

    private val server = LightwaveServer()
    private val executor = OperationExecutor(server)
    private val deviceChangeHandler = DeviceChangeHandler(server)

    private var selectedRoom : String = "Living room"
    private var selectedSwitchFeature : String = FEATURE_LIVING_ROOM_SWITCH_ID
    private var selectedDimFeature : String = FEATURE_LIVING_ROOM_DIM_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        setupLocationSelectors()
        setupOnOffSwitches()
        setupDimmer()

        executor.connectToServer(WeakReference(applicationContext))
        deviceChangeHandler.addListener(this)
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
        }
    }

    private fun setupOnOffSwitches() {
        val offButton = findViewById<Button>(R.id.off_button)
        val onButton = findViewById<Button>(R.id.on_button)

        offButton.setOnClickListener {
            executor.enqueue(selectedSwitchFeature, 0)
            executor.enqueue(selectedDimFeature, 0) // Workaround for a bug in my dimmer bulbs :)
            applyOnOffHighlight(false)
        }

        onButton.setOnClickListener {
            executor.enqueue(selectedSwitchFeature, 1)
            executor.enqueue(selectedDimFeature, 100) // Workaround for a bug in my dimmer bulbs :)
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
            executor.enqueue(selectedDimFeature, newValue)
            executor.enqueue(selectedSwitchFeature, if (newValue > 0) 1 else 0)
            applyOnOffHighlight(newValue > 0)
        };
    }

    override fun onDeviceChanged(featureId: String, newValue: Int) {
        Log.d(javaClass.name, "Device change detected: $featureId=$newValue")
        if (featureId.equals(selectedDimFeature)) {
            runOnUiThread {
                val croller = findViewById<View>(R.id.croller) as Croller
                croller.progress = newValue
                applyOnOffHighlight(croller.progress > 0)
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
