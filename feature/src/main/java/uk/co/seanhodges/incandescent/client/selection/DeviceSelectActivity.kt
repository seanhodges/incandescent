package uk.co.seanhodges.incandescent.client.selection

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import uk.co.seanhodges.incandescent.client.storage.*
import uk.co.seanhodges.incandescent.client.support.GatherDeviceReport
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference

class DeviceSelectActivity(
        private val server: LightwaveServer = Inject.server,
        private val executor: OperationExecutor = Inject.executor
) : AppCompatActivity() {

    private lateinit var viewModel: DeviceSelectViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var contentAdapter: ContentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_select)

        viewModel = ViewModelProviders.of(this).get(DeviceSelectViewModel::class.java)

        contentAdapter = ContentAdapter()
        recyclerView = this.findViewById<RecyclerView>(R.id.roomList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contentAdapter

        viewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> {
            roomsWithDevices -> contentAdapter.setData(roomsWithDevices)
        })

        executor.reportHandler = { packet ->
            //@see OperationExecutor.onRawEvent()
            GatherDeviceReport(this).saveReport(packet)
        }
    }

    private fun setupActionBar() {
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setLogo(R.mipmap.ic_launcher)
        supportActionBar?.title = getString(R.string.select_device_title)
    }

    override fun onResume() {
        super.onResume()

        val authRepository = AuthRepository(WeakReference(applicationContext))
        if (!authRepository.isAuthenticated()) {
            startActivity(Intent(this, AuthenticateActivity::class.java))
        }
        else {
            executor.connectToServer(authRepository, onComplete = { success: Boolean ->
                if (success) {
                    viewModel.initialiseRooms(server)
                }
                else {
                    Toast.makeText(this, "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val settingsRepository = SettingsRepository(WeakReference(applicationContext))
        val settings = settingsRepository.get()
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_device_select, menu)
        val compactView = menu.findItem(R.id.compact_view)
        compactView?.isChecked = settings.deviceListSize == DeviceListSize.SMALL
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.compact_view -> {
                val deviceListSize = if (item.isChecked) DeviceListSize.LARGE else DeviceListSize.SMALL
                item.isChecked = deviceListSize == DeviceListSize.SMALL
                recyclerView.adapter = contentAdapter
                val settingsRepository = SettingsRepository(WeakReference(applicationContext))
                settingsRepository.save(AppSettings(deviceListSize))
                return true;
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        server.disconnect()
        finish()
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
        for (device in roomData[position].getDevicesInOrder()) {
            val deviceView = ListEntry.Builder(this.parent).title(device.title).type(device.type).build()
            deviceView.setOnClickListener {
                val intent = Intent(parent.context, DeviceControlActivity::class.java)
                intent.putExtra("selectedRoom", room)
                intent.putExtra("selectedDevice", device)
                parent.context.startActivity(intent)
            }
            deviceList.addView(deviceView)
        }
    }

    override fun getItemCount(): Int {
        return roomData.size
    }
}

class RoomViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)