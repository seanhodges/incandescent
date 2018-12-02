package uk.co.seanhodges.incandescent.client.selection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.Inject.executor
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import uk.co.seanhodges.incandescent.client.scene.AddSceneActivity
import uk.co.seanhodges.incandescent.client.scene.ApplyScene
import uk.co.seanhodges.incandescent.client.storage.*
import uk.co.seanhodges.incandescent.client.support.GatherDeviceReport
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference

class DeviceSelectActivity(
        private val server: LightwaveServer = Inject.server,
        private val executor: OperationExecutor = Inject.executor
) : AppCompatActivity() {

    private lateinit var sceneViewModel: SceneViewModel
    private lateinit var deviceViewModel: DeviceSelectViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var contentAdapter: ContentAdapter

    // FIXME: Avoids an infinite loop when the DB observer is triggered by the device refresh commands
    // Ideally we fetch the devices outside the observer and trigger observe only after they've finished
    private var firstTimeLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_select)

        contentAdapter = ContentAdapter()
        recyclerView = this.findViewById<RecyclerView>(R.id.room_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contentAdapter

        sceneViewModel = ViewModelProviders.of(this).get(SceneViewModel::class.java)
        sceneViewModel.getAllScenes().observe(this, Observer<List<SceneWithActions>> {
            scenesWithActions -> contentAdapter.setSceneData(scenesWithActions)
        })
        deviceViewModel = ViewModelProviders.of(this).get(DeviceSelectViewModel::class.java)
        deviceViewModel.listenForValueChanges(this)
        deviceViewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> { roomsWithDevices ->
            contentAdapter.setDeviceData(roomsWithDevices)
            if (firstTimeLoad) {
                firstTimeLoad = false
                refreshDeviceValues(roomsWithDevices)
            }
        })

        executor.reportHandler = { packet ->
            //@see OperationExecutor.onRawEvent()
            GatherDeviceReport(this).saveReport(packet)
        }
    }

    private fun refreshDeviceValues(roomsWithDevices: List<RoomWithDevices>) {
        roomsWithDevices.forEach { room ->
            executor.enqueueLoadAll(room.devices!!.flatMap {
                arrayOf(it.powerCommand, it.dimCommand).filterNotNull()
            })
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
                    deviceViewModel.initialiseRooms(server)
                }
                else {
                    Toast.makeText(this, "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, AuthenticateActivity::class.java))
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val settingsRepository = SettingsRepository(WeakReference(applicationContext))
        val settings = settingsRepository.get()
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_device_select, menu)
        val compactView = menu.findItem(R.id.action_compact_view)
        compactView?.isChecked = settings.deviceListSize == DeviceListSize.SMALL

        for (i in 0.until(menu.size())) {
            // Forward custom actionbar controls to the normal options item handler
            val item = menu.getItem(i)
            if (item.itemId == R.id.action_show_only_active) {
                val switch = item.actionView.findViewById<Switch>(R.id.show_only_active_switch)
                switch.setOnClickListener {
                    when (it) {
                        is Switch -> item.isChecked = it.isChecked
                    }
                    onOptionsItemSelected(item)
                }
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_show_only_active -> {
                Log.d(javaClass.name, "Setting active only filter to ${item.isChecked}")
                contentAdapter.setFilters(item.isChecked)
                return true;
            }
            R.id.action_compact_view -> {
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


private const val VIEW_TYPE_SCENE = 0
private const val VIEW_TYPE_ROOM = 1

class ContentAdapter() : RecyclerView.Adapter<SectionViewHolder>() {

    private var sceneData: List<SceneWithActions> = emptyList()
    private var roomData: List<RoomWithDevices> = emptyList()
    private lateinit var parentView: ViewGroup
    private var activeOnly = false

    fun setSceneData(newData: List<SceneWithActions>) {
        this.sceneData = newData
        notifyDataSetChanged()
    }

    fun setDeviceData(newData: List<RoomWithDevices>) {
        this.roomData = newData
        notifyDataSetChanged()
    }

    fun setFilters(activeOnly: Boolean) {
        this.activeOnly = activeOnly
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) : Int = when(position) {
        0 -> VIEW_TYPE_SCENE
        else -> VIEW_TYPE_ROOM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        this.parentView = parent
        val view = when(viewType) {
            VIEW_TYPE_SCENE -> LayoutInflater.from(parent.context).inflate(R.layout.content_scene_entry, parent, false)
            else -> LayoutInflater.from(parent.context).inflate(R.layout.content_room_entry, parent, false)
        }
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        if (position == 0) {
            // Always draw scenes at the top
            renderScenes(holder)
        } else {
            renderDevices(holder, position - 1)
        }
    }

    private fun renderScenes(holder: SectionViewHolder) {
        val buttonList : LinearLayout = holder.containerView.findViewById(R.id.device_list)
        buttonList.removeAllViewsInLayout()

        for (sceneWithActions in sceneData) {
            val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_list_entry, this.parentView, false) as Button
            val item = ListEntryDecorator(button, this.parentView)
                    .title(sceneWithActions.scene?.title ?: "")
                    .type("scene")
                    .build()
            buttonList.addView(item)
            item.setOnClickListener {
                ApplyScene(this.parentView.context, executor)
                        .execute(sceneWithActions.scene?.id!!)
            }
        }

        val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_list_entry, this.parentView, false) as Button
        val item = ListEntryDecorator(button, this.parentView)
                .title(parentView.resources.getString(R.string.add_new_scene_label))
                .type("add")
                .build()
        buttonList.addView(item)
        item.setOnClickListener {
            this.parentView.context.startActivity(Intent(this.parentView.context, AddSceneActivity::class.java))
        }
    }

    private fun renderDevices(holder: SectionViewHolder, position: Int) {
        val room = roomData[position].room
        val roomTitle : TextView = holder.containerView.findViewById(R.id.room_title)
        roomTitle.text = room?.title

        val buttonList : LinearLayout = holder.containerView.findViewById(R.id.device_list)
        buttonList.removeAllViewsInLayout()

        roomData[position].getDevicesInOrder().forEach { device ->
            if (activeOnly && device.lastPowerValue == 0) {
                return@forEach
            }

            val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_list_entry, this.parentView, false) as Button
            val item = ListEntryDecorator(button, this.parentView)
                    .title(device.title)
                    .type(device.type)
                    .active(device.lastPowerValue == 1)
                    .build()
            item.setOnClickListener {
                val intent = Intent(this.parentView.context, DeviceControlActivity::class.java)
                intent.putExtra("selectedRoom", room)
                intent.putExtra("selectedDevice", device)
                this.parentView.context.startActivity(intent)
            }
            buttonList.addView(item)
        }

        // If there are no devices, collapse the row
        setVisibility(if (buttonList.childCount == 0) View.GONE else View.VISIBLE, roomTitle, buttonList)
    }

    private fun setVisibility(visibility: Int, vararg views: View) {
        views.forEach {
            it.visibility = visibility
        }
    }

    override fun getItemCount(): Int {
        return roomData.size + 1
    }
}

class SectionViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)