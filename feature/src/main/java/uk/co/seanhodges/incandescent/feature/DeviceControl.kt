package uk.co.seanhodges.incandescent.feature

import android.content.Context
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class DeviceControl : AppCompatActivity() {

    private val server = LightwaveServer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        setupOnOffSwitches()
        setupDimmer()
        ConnectToServer(WeakReference(applicationContext)).execute(server)
    }

    fun setupOnOffSwitches() {
        val offButton = findViewById<Button>(R.id.off_button)
        offButton.setOnClickListener {
            val operation = LWOperation("feature", "write")
            operation.addPayload(LWOperationPayloadFeature(FEATURE_SWITCH_ID, 0))
            server.command(operation)
        }

        val onButton = findViewById<Button>(R.id.on_button)
        onButton.setOnClickListener {
            val operation = LWOperation("feature", "write")
            operation.addPayload(LWOperationPayloadFeature(FEATURE_SWITCH_ID, 1))
            server.command(operation)
        }
    }

    fun setupDimmer() {
        val croller = findViewById<View>(R.id.croller) as Croller
        croller.indicatorWidth = 10f
        croller.backCircleColor = Color.parseColor("#EDEDED")
        croller.mainCircleColor = Color.WHITE
        croller.max = 50
        croller.startOffset = 45
        croller.setIsContinuous(false)
        croller.labelColor = Color.BLACK
        croller.progressPrimaryColor = Color.parseColor("#0B3C49")
        croller.indicatorColor = Color.parseColor("#0B3C49")
        croller.progressSecondaryColor = Color.parseColor("#EEEEEE")

        croller.setOnCrollerChangeListener(object : OnCrollerChangeListener {

            override fun onProgressChanged(croller: Croller, progress: Int) {
                // use the progress
                val operation = LWOperation("feature", "write")
                operation.addPayload(LWOperationPayloadFeature(FEATURE_DIM_ID, progress))
                server.command(operation)
            }

            override fun onStartTrackingTouch(croller: Croller) {
                // tracking started
                // TODO(sean): stop event updates
            }

            override fun onStopTrackingTouch(croller: Croller) {
                // tracking stopped
                // TODO(sean): continue event updates
            }
        });
    }

    companion object {

        private val FEATURE_SWITCH_ID = "5b8aa9b4d36c330fd5b4e100-22-3157332334+1"
        private val FEATURE_DIM_ID = "5b8aa9b4d36c330fd5b4e100-23-3157332334+1"
    }
}
