package uk.co.seanhodges.incandescent.client.scene

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.R

import kotlinx.android.synthetic.main.activity_add_scene.*
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices

class AddSceneActivity : AppCompatActivity() {

    private lateinit var sceneViewModel: AddSceneViewModel
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

        sceneViewModel = ViewModelProviders.of(this).get(AddSceneViewModel::class.java)
        sceneViewModel.listenForValueChanges(this)
        sceneViewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> {
            roomsWithDevices -> contentAdapter.setDeviceData(roomsWithDevices)
        })

        val sceneName = findViewById<EditText>(R.id.scene_name)

        fab.setOnClickListener { view ->
            // Create the new scene and close
            val name = sceneName.text.toString()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please give the scene a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sceneViewModel.save(name, contentAdapter.getEnabledDeviceData())
            finish()
        }
    }

    private fun setupActionBar() {
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

class ContentAdapter() : RecyclerView.Adapter<SectionViewHolder>() {

    private var deviceData: MutableList<FlatDeviceRow> = mutableListOf()
    private lateinit var parentView: ViewGroup

    fun setDeviceData(newData: List<RoomWithDevices>) {
        newData.forEach { room ->
            room.devices?.forEach { device ->
                val title = "${room.room?.title} > ${device.title}"
                this.deviceData.add(FlatDeviceRow(title, device))
            }
        }
        notifyDataSetChanged()
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