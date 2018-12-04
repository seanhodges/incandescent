package uk.co.seanhodges.incandescent.client.scene

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices

class ContentAdapter : RecyclerView.Adapter<SectionViewHolder>() {

    private var deviceData: MutableList<FlatDeviceRow> = mutableListOf()
    private lateinit var parentView: ViewGroup

    fun setDeviceData(newData: List<RoomWithDevices>) {
        if (this.deviceData.size > 0) {
            return // FIXME: Ignoring device data updates to avoid rebuilding list
        }
        this.deviceData.clear()
        newData.forEach { room ->
            room.devices?.forEach { device ->
                val title = buildLabel(device, room)
                this.deviceData.add(FlatDeviceRow(title, device))
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_scene_setting, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val device = deviceData[position]

        val title : TextView = holder.containerView.findViewById(R.id.scene_setting_name)
        title.text = device.title

        holder.containerView.findViewById<Switch>(R.id.use_device).setOnCheckedChangeListener { _, checked ->
            device.enabled = checked
        }
    }

    override fun getItemCount(): Int {
        return deviceData.size
    }
}

data class FlatDeviceRow(
        val title: String,
        val device: DeviceEntity
) {
    var enabled: Boolean = false
}

class SectionViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)