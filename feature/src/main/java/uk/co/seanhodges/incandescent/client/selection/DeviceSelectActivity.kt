package uk.co.seanhodges.incandescent.client.selection

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import java.lang.ref.WeakReference

private const val DEVICE_BUTTON_IMAGE_SIZE = 72

class DeviceSelectActivity : AppCompatActivity() {

    private lateinit var repository: DeviceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_select)

        repository = DeviceRepository(WeakReference(this))

        val recyclerView = this.findViewById<RecyclerView>(R.id.roomList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ContentAdapter(repository.getAllRooms())
    }
}

class ContentAdapter(private val roomData: List<RoomWithDevices>) : RecyclerView.Adapter<RoomViewHolder>() {

    private lateinit var parent: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        this.parent = parent
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_room_entry, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomData[position].room
        val roomTitle : TextView = holder.containerView.findViewById(R.id.roomTitle)
        roomTitle.text = room?.title

        val deviceList : LinearLayout = holder.containerView.findViewById(R.id.deviceList)
        for (device in roomData[position].devices ?: emptyList()) {
            val deviceView = createNewDeviceView(device)
            deviceView.setOnClickListener {
                parent.context.startActivity(Intent(parent.context, DeviceControlActivity::class.java))
            }
            deviceList.addView(deviceView)
        }
    }

    private fun createNewDeviceView(device : DeviceEntity): View {
        val button: TextView = LayoutInflater.from(parent.context).inflate(R.layout.content_device_entry, parent, false) as TextView
        button.text = device.text
        val image = getDeviceButtonImage(device.type)
        val imageSizePx = (DEVICE_BUTTON_IMAGE_SIZE * parent.resources.displayMetrics.density).toInt()
        image.setBounds(0, 0, imageSizePx, imageSizePx)
        button.setCompoundDrawablesRelative(null, image, null, null)
        return button
    }

    private fun getDeviceButtonImage(id: String) = when(id) {
        "light" -> parent.resources.getDrawable(R.drawable.device_button_lightbulb, null)
        else -> parent.resources.getDrawable(R.drawable.device_button_socket, null)
    }

    override fun getItemCount(): Int {
        return roomData.size
    }
}

class RoomViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)