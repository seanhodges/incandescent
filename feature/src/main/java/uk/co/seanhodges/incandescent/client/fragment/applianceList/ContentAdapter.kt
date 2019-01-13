package uk.co.seanhodges.incandescent.client.fragment.applianceList

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.IconResolver
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LaunchActivity
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.selection.DEVICE_BUTTON_HIGHLIGHT_LENGTH
import uk.co.seanhodges.incandescent.client.selection.ENTRY_ACTIVE_COLOUR
import uk.co.seanhodges.incandescent.client.selection.ENTRY_DEFAULT_COLOUR
import uk.co.seanhodges.incandescent.client.selection.ENTRY_SELECTED_COLOUR
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices

class ContentAdapter(
        private val launch: LaunchActivity = Inject.launch
) : RecyclerView.Adapter<SectionViewHolder>() {

    private var deviceData: MutableList<FlatDeviceRow> = mutableListOf()
    private lateinit var parentView: ViewGroup

    fun setDeviceData(newData: List<RoomWithDevices>, enabled: List<String> = emptyList()) {
        this.deviceData.clear()
        newData.forEach { room ->
            room.devices?.forEach { device ->
                val entry = FlatDeviceRow(buildLabel(device, room), room.room!!, device)
                if (enabled.contains(device.id)) entry.enabled = true
                this.deviceData.add(entry)
            }
        }
        notifyDataSetChanged()
    }

    private fun buildLabel(device: DeviceEntity, room: RoomWithDevices): String {
        val status = when (device.lastPowerValue) {
            1 -> {
                if (device.lastDimValue > 0) {
                    "On, ${device.lastDimValue}%"
                } else {
                    "On"
                }
            }
            else -> "Off"
        }
        return "${room.room?.title} > ${device.title} ($status)"
    }

    fun getEnabledDeviceData(): List<FlatDeviceRow> {
        return deviceData.filter { it.enabled }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        this.parentView = parent
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_select_list_entry, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val rowData = deviceData[position]

        val row = holder.containerView
        row.findViewById<Switch>(R.id.action_enable).visibility = View.GONE

        val deviceImage: ImageView = row.findViewById(R.id.device_image)
        deviceImage.setImageResource(IconResolver.getDeviceImage(rowData.device.title, rowData.device.type))
        if (rowData.device.lastPowerValue == 1) {
            deviceImage.imageTintList = ColorStateList.valueOf(Color.parseColor(ENTRY_ACTIVE_COLOUR))
        }

        val title : TextView = holder.containerView.findViewById(R.id.device_name)
        title.text = rowData.title

        val pick = row.findViewById<CheckBox>(R.id.action_pick)
        pick.visibility = View.VISIBLE
        pick.isChecked = rowData.enabled
        pick.setOnCheckedChangeListener { _, checked ->
            rowData.enabled = checked
        }

        val controlButton = row.findViewById<ImageButton>(R.id.control_button)
        controlButton.setOnClickListener {
            launch.deviceControl(this.parentView.context, rowData.room, rowData.device)
        }
        controlButton.setOnTouchListener(applyControlButtonPressEffect(controlButton))
    }

    override fun getItemCount(): Int {
        return deviceData.size
    }

    private fun applyControlButtonPressEffect(button: ImageButton) : View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setButtonColour(button, ENTRY_SELECTED_COLOUR)
                    Handler().postDelayed({
                        setButtonColour(button, ENTRY_DEFAULT_COLOUR)
                    }, DEVICE_BUTTON_HIGHLIGHT_LENGTH)
                }
            }
            return@OnTouchListener false
        }
    }

    private fun setButtonColour(button: ImageButton, colour: String) {
        button.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colour))
    }
}

data class FlatDeviceRow(
        val title: String,
        val room: RoomEntity,
        val device: DeviceEntity
) {
    var enabled: Boolean = false
}

class SectionViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)