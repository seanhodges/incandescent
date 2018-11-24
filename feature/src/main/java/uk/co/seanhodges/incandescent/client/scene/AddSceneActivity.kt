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
import uk.co.seanhodges.incandescent.client.selection.DeviceSelectViewModel
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices

class AddSceneActivity : AppCompatActivity() {

    private lateinit var deviceViewModel: DeviceSelectViewModel
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

        deviceViewModel = ViewModelProviders.of(this).get(DeviceSelectViewModel::class.java)
        deviceViewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> {
            roomsWithDevices -> contentAdapter.setDeviceData(roomsWithDevices)
        })

        fab.setOnClickListener { view ->
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

    private var roomData: List<RoomWithDevices> = emptyList()
    private lateinit var parentView: ViewGroup

    fun setDeviceData(newData: List<RoomWithDevices>) {
        this.roomData = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        this.parentView = parent
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_scene_setting, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val room = roomData[position].room

        val item: View = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_scene_setting, this.parentView, false)

        val title : TextView = item.findViewById(R.id.scene_setting_name)
        title.text = room?.title

        item.findViewById<Switch>(R.id.use_device).setOnClickListener {
            Toast.makeText(this.parentView.context, "Clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return roomData.size
    }
}

class SectionViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)