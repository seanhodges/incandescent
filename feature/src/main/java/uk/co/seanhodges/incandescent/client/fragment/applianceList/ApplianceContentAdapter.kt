package uk.co.seanhodges.incandescent.client.fragment.applianceList

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
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
import uk.co.seanhodges.incandescent.client.fragment.SectionViewHolder
import uk.co.seanhodges.incandescent.client.selection.DEVICE_BUTTON_HIGHLIGHT_LENGTH
import uk.co.seanhodges.incandescent.client.selection.ENTRY_ACTIVE_COLOUR
import uk.co.seanhodges.incandescent.client.selection.ENTRY_DEFAULT_COLOUR
import uk.co.seanhodges.incandescent.client.selection.ENTRY_SELECTED_COLOUR
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices

class ApplianceContentAdapter(
        private val launch: LaunchActivity = Inject.launch,
        var theme: Theme = Theme.Light // TODO: Move this to a material theme
) : RecyclerView.Adapter<SectionViewHolder>() {

    enum class Theme {
        Light,
        Dark
    }

    private var applianceData: MutableList<FlatApplianceRow> = mutableListOf()
    private lateinit var parentView: ViewGroup

    fun setDeviceData(newData: List<RoomWithDevices>, enabled: List<String> = emptyList()) {
        this.applianceData.clear()
        newData.forEach { room ->
            room.getVisibleDevices().forEach { appliance ->
                val entry = FlatApplianceRow(buildLabel(appliance, room), room.room!!, appliance)
                if (enabled.contains(appliance.id)) entry.enabled = true
                this.applianceData.add(entry)
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

    fun setEnabledSceneData(enabled: List<String>) {
        applianceData.forEach { appliance ->
            appliance.enabled = enabled.contains(appliance.appliance.id)
        }
        notifyDataSetChanged()
    }

    fun getEnabledDeviceData(): List<FlatApplianceRow> {
        return applianceData.filter { it.enabled }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        this.parentView = parent
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_select_list_entry, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val rowData = applianceData[position]

        val row = holder.containerView
        row.findViewById<Switch>(R.id.action_enable).visibility = View.GONE

        val deviceImage: ImageView = row.findViewById(R.id.device_image)
        applyThemeGraphic(deviceImage)
        deviceImage.setImageResource(IconResolver.getDeviceImage(rowData.appliance.title, rowData.appliance.type))
        if (rowData.appliance.lastPowerValue == 1) {
            deviceImage.imageTintList = ColorStateList.valueOf(Color.parseColor(ENTRY_ACTIVE_COLOUR))
        }

        val title : TextView = holder.containerView.findViewById(R.id.device_name)
        applyThemeText(title)
        title.text = rowData.title

        val pick = row.findViewById<CheckBox>(R.id.action_pick)
        pick.visibility = View.VISIBLE
        pick.isChecked = rowData.enabled
        pick.setOnCheckedChangeListener { _, checked ->
            rowData.enabled = checked
        }

        val controlButton = row.findViewById<ImageButton>(R.id.control_button)
        applyThemeGraphic(controlButton)
        controlButton.setOnClickListener {
            launch.deviceControl(this.parentView.context, rowData.room, rowData.appliance)
        }
        controlButton.setOnTouchListener(applyControlButtonPressEffect(controlButton))
    }

    private fun applyThemeText(component: TextView) {
        if (theme == Theme.Dark) {
            component.setTextColor(ColorStateList.valueOf(parentView.resources.getColor(R.color.theme_light_list_fg_colour, null)))
        }
    }

    private fun applyThemeGraphic(component: ImageView) {
        if (theme == Theme.Dark) {
            component.imageTintList = ColorStateList.valueOf(parentView.resources.getColor(R.color.theme_light_list_fg_colour, null))
            component.imageTintMode = PorterDuff.Mode.SRC_IN
            component.backgroundTintList = ColorStateList.valueOf(parentView.resources.getColor(R.color.theme_light_list_fg_colour, null))
            component.backgroundTintMode = PorterDuff.Mode.SRC_IN
        }
    }

    override fun getItemCount(): Int {
        return applianceData.size
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

data class FlatApplianceRow(
        val title: String,
        val room: RoomEntity,
        val appliance: DeviceEntity
) {
    var enabled: Boolean = false
}