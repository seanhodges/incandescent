package uk.co.seanhodges.incandescent.client.scene

import android.os.Bundle
import android.view.MenuItem
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_add_scene.*
import uk.co.seanhodges.incandescent.client.*
import uk.co.seanhodges.incandescent.client.fragment.applianceList.ApplianceListViewModel
import uk.co.seanhodges.incandescent.client.fragment.applianceList.ContentAdapter
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices


class AddSceneActivity(
        private val executor: OperationExecutor = Inject.executor
) : AppCompatActivity(), ConnectionAware {

    private lateinit var connectionMonitor: ConnectionStateMonitor
    private lateinit var sceneViewModel: ApplianceListViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var contentAdapter: ContentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_scene)

        contentAdapter = ContentAdapter()
        recyclerView = this.findViewById(R.id.scene_setting_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contentAdapter

        sceneViewModel = ViewModelProviders.of(this).get(ApplianceListViewModel::class.java)
        sceneViewModel.listenForValueChanges(this)
        sceneViewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> {
            roomsWithDevices -> contentAdapter.setDeviceData(roomsWithDevices)
        })

        val sceneName = findViewById<EditText>(R.id.scene_name)

        fab.setOnClickListener { _ ->
            doAddScene(sceneName)
        }

        connectionMonitor = ConnectionStateMonitor(this, this)
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

    private fun doAddScene(sceneName: EditText) {
        // Create the new scene and close
        val name = sceneName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please give the scene a name", Toast.LENGTH_SHORT).show()
            return
        }


        SaveSceneTask(this).execute(AddSceneForm(name, contentAdapter.getEnabledDeviceData()))
        finish()
    }

    private fun setupActionBar() {
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}