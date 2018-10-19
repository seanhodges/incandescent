package uk.co.seanhodges.incandescent.client.selection

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.auth.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch


private const val DEVICE_BUTTON_IMAGE_SIZE = 72

class DeviceSelectActivity : AppCompatActivity() {

    private val server = Inject.server
    private val executor = Inject.executor

    private lateinit var repository: DeviceRepository
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_select)

        recyclerView = this.findViewById<RecyclerView>(R.id.roomList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ContentAdapter()
    }

    override fun onPostResume() {
        super.onPostResume()

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

                GetRoomsTask(this, server, recyclerView.adapter as ContentAdapter).execute()
            })
        }
    }

    override fun onPause() {
        super.onPause()

        server.disconnect()
    }
}

private class GetRoomsTask(
        ctx : Context,
        private val server: LightwaveServer,
        private val adapter: ContentAdapter
) : AsyncTask<Void, Void, List<RoomWithDevices>>() {

    private val ctxRef: WeakReference<Context> = WeakReference(ctx)
    private val doneSignal = CountDownLatch(1)

    override fun doInBackground(vararg params: Void): List<RoomWithDevices>? {
        val ctx = ctxRef.get() ?: return emptyList()

        val repository = DeviceRepository(ctx)
        if (repository.isNewDB()) {
            LightwaveConfigLoader(server, repository).load { hierarchy: String, info: LWEventPayloadGroup ->
                LightwaveConfigParser(hierarchy, info).parse { room, devices ->
                    repository.addRoomAndDevices(RoomWithDevices(room, devices))
                }
                doneSignal.countDown();
            }
            doneSignal.await()
        }
        return repository.getAllRooms()
    }

    override fun onPostExecute(result: List<RoomWithDevices>?) {
        super.onPostExecute(result)
        adapter.setData(result ?: emptyList())
    }
}

class ContentAdapter() : RecyclerView.Adapter<RoomViewHolder>() {

    private var roomData: List<RoomWithDevices> = emptyList()
    private lateinit var parent: ViewGroup

    fun setData(newData: List<RoomWithDevices>) {
        this.roomData = newData
        notifyDataSetChanged()
    }

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
        deviceList.removeAllViewsInLayout()
        for (device in roomData[position].devices ?: emptyList()) {
            val deviceView = createNewDeviceView(device)
            deviceView.setOnClickListener {
                val intent = Intent(parent.context, DeviceControlActivity::class.java)
                intent.putExtra("selectedRoom", room)
                intent.putExtra("selectedDevice", device)
                parent.context.startActivity(intent)
            }
            deviceList.addView(deviceView)
        }
    }

    private fun createNewDeviceView(device : DeviceEntity): View {
        val button: TextView = LayoutInflater.from(parent.context).inflate(R.layout.content_device_entry, parent, false) as TextView
        button.text = device.title
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