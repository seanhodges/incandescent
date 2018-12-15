package uk.co.seanhodges.incandescent.client.selection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LaunchActivity
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.*
import uk.co.seanhodges.incandescent.client.support.GatherDeviceReport
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference

class DeviceSelectActivity(
        private val launch: LaunchActivity = Inject.launch,
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
        if (isNetworkDown()) {
            val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            builder.setTitle(resources.getString(R.string.alert_title_no_internet))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setMessage(resources.getString(R.string.alert_message_no_internet))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        finish()
                        startActivity(intent)
                    }
                    .show()
            return
        }

        val authRepository = AuthRepository(WeakReference(applicationContext))
        if (!authRepository.isAuthenticated()) {
            launch.authenticate(this)
        }
        else {
            executor.connectToServer(authRepository, onComplete = { success: Boolean ->
                if (success) {
                    deviceViewModel.initialiseList(this)
                }
                else {
                    Toast.makeText(this, "Could not connect to Lightwave server :(", Toast.LENGTH_LONG).show()
                    launch.authenticate(this)
                }
            })
        }

        firstTimeLoad = true
    }

    private fun isNetworkDown(): Boolean {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting != true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val settingsRepository = SettingsRepository(WeakReference(applicationContext))
        val settings = settingsRepository.get()
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_device_select, menu)
        var item = menu.findItem(R.id.action_compact_view)
        item?.isChecked = settings.deviceListSize == DeviceListSize.SMALL
        item = menu.findItem(R.id.action_show_only_active)
        item?.isChecked = settings.showOnlyActiveDevices
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_show_only_active -> {
                item.isChecked = !item.isChecked()
                Log.d(javaClass.name, "Setting active only filter to ${item.isChecked}")
                contentAdapter.setFilters(item.isChecked)
                val settingsRepository = SettingsRepository(WeakReference(applicationContext))
                settingsRepository.updateShowOnlyActiveDevices(item.isChecked)
                return true;
            }
            R.id.refresh_appliances -> {
                deviceViewModel.refreshList(this)
                return true
            }
            R.id.action_compact_view -> {
                item.isChecked = !item.isChecked()
                val deviceListSize = if (item.isChecked) DeviceListSize.SMALL else DeviceListSize.LARGE
                recyclerView.adapter = contentAdapter
                val settingsRepository = SettingsRepository(WeakReference(applicationContext))
                settingsRepository.updateDeviceListSize(deviceListSize)
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