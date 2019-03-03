package uk.co.seanhodges.incandescent.client.selection

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robinhood.spark.SparkView
import com.robinhood.spark.animation.MorphSparkAnimator
import uk.co.seanhodges.incandescent.client.*
import uk.co.seanhodges.incandescent.client.storage.DeviceViewMode
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices
import uk.co.seanhodges.incandescent.client.storage.SceneWithActions
import uk.co.seanhodges.incandescent.client.storage.SettingsRepository
import uk.co.seanhodges.incandescent.client.support.GatherDeviceReport
import java.lang.ref.WeakReference

class DeviceSelectActivity(
        private val launch: LaunchActivity = Inject.launch,
        private val executor: OperationExecutor = Inject.executor
) : AppCompatActivity(), ConnectionAware, EnergyAware {

    private lateinit var connectionMonitor: ConnectionStateMonitor
    private lateinit var sceneViewModel: SceneViewModel
    private lateinit var deviceViewModel: DeviceSelectViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var contentAdapter: ContentAdapter

    private lateinit var menu: Menu

    // FIXME: Avoids an infinite loop when the DB observer is triggered by the device refresh commands
    // Ideally we fetch the devices outside the observer and trigger observe only after they've finished
    private var firstTimeLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        val settingsRepository = SettingsRepository(WeakReference(applicationContext))
        contentAdapter = ContentAdapter(launch, executor, settingsRepository.get().showOnlyActiveDevices)

        recyclerView = this.findViewById(R.id.room_list)
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

        val spark = this.findViewById<SparkView>(R.id.energy_monitor)
        spark.sparkAnimator = MorphSparkAnimator()
        RollingSparkAdapter(this, 10, 200).let { adapter ->
            spark.adapter = adapter
            EnergyMonitor(adapter, this).start()
            spark.isScrubEnabled = true
            spark.setScrubListener {
                adapter.reset()
            }
        }

        executor.reportHandler = { packet ->
            //@see OperationExecutor.onRawEvent()
            GatherDeviceReport(this).saveReport(packet)
        }

        connectionMonitor = ConnectionStateMonitor(this, this)
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
        firstTimeLoad = true
    }

    override fun onAuthenticationSuccess() {
        deviceViewModel.initialiseList(this)
    }

    override fun onAuthenticationFailed() {
        launch.authenticate(this)
    }

    override fun onConnectionAvailable() {
        this.runOnUiThread {
            this.findViewById<TextView>(R.id.no_network_alert)?.visibility = TextView.GONE
        }
    }

    override fun onConnectionLost() {
        this.runOnUiThread {
            this.findViewById<TextView>(R.id.no_network_alert)?.visibility = TextView.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onEnergyChanged(wattsValue: Int, kwhValue: Float) {
        runOnUiThread {
            val currentWatts = this.findViewById<TextView>(R.id.energy_monitor_watts)
            currentWatts.text = "$wattsValue W"

            val currentKWH = this.findViewById<TextView>(R.id.energy_monitor_kwh)
            currentKWH.text = "%.1f".format(kwhValue) + " kWh"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        val settingsRepository = SettingsRepository(WeakReference(applicationContext))
        val settings = settingsRepository.get()
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_device_select, menu)
        var item = menu.findItem(R.id.action_view_mode)
        item?.isChecked = settings.deviceViewMode == DeviceViewMode.LIST
        item?.setIcon(if (item.isChecked) R.drawable.list_action_button_on else R.drawable.list_action_button_off)
        contentAdapter.setViewMode(settings.deviceViewMode)
        item = menu.findItem(R.id.action_show_only_active)
        item?.isChecked = settings.showOnlyActiveDevices
        item?.setIcon(if (item.isChecked) R.drawable.filter_action_button_on else R.drawable.filter_action_button_off)
        contentAdapter.setFilters(settings.showOnlyActiveDevices)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_show_only_active -> {
                item.isChecked = !item.isChecked
                item.setIcon(if (item.isChecked) R.drawable.filter_action_button_on else R.drawable.filter_action_button_off)
                contentAdapter.setFilters(item.isChecked)
                val settingsRepository = SettingsRepository(WeakReference(applicationContext))
                settingsRepository.updateShowOnlyActiveDevices(item.isChecked)
                return true;
            }
            R.id.refresh_appliances -> {
                deviceViewModel.refreshList(this)
                return true
            }
            R.id.action_view_mode -> {
                item.isChecked = !item.isChecked
                item.setIcon(if (item.isChecked) R.drawable.list_action_button_on else R.drawable.list_action_button_off)
                val deviceViewMode = if (item.isChecked) DeviceViewMode.LIST else DeviceViewMode.GRID
                contentAdapter.setViewMode(deviceViewMode)
                val settingsRepository = SettingsRepository(WeakReference(applicationContext))
                settingsRepository.updateDeviceViewMode(deviceViewMode)
                return true;
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        executor.stop()
        finish()
    }
}