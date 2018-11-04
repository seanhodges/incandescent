package uk.co.seanhodges.incandescent.client.selection

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import uk.co.seanhodges.incandescent.client.IconResolver
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.AuthRepository
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference

private const val DEVICE_BUTTON_IMAGE_SIZE : Int = 72
private const val DEVICE_BUTTON_HIGHLIGHT_LENGTH : Long = 300

class DeviceSelectActivity(
        private val server: LightwaveServer = Inject.server,
        private val executor: OperationExecutor = Inject.executor
) : AppCompatActivity() {

    private lateinit var viewModel: DeviceSelectViewModel
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        setupActionBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_select)

        viewModel = ViewModelProviders.of(this).get(DeviceSelectViewModel::class.java)

        val contentAdapter = ContentAdapter()
        recyclerView = this.findViewById<RecyclerView>(R.id.roomList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contentAdapter

        viewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> {
            roomsWithDevices -> contentAdapter.setData(roomsWithDevices)
        })
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
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
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

    @SuppressLint("ClickableViewAccessibility")
    private fun createNewDeviceView(device : DeviceEntity): View {
        val button: Button = LayoutInflater.from(parent.context).inflate(R.layout.content_device_entry, parent, false) as Button
        button.text = device.title
        val image = parent.resources.getDrawable(IconResolver.getDeviceImage(device.title, device.type), null)
        val imageSizePx = (DEVICE_BUTTON_IMAGE_SIZE * parent.resources.displayMetrics.density).toInt()
        image.setBounds(0, 0, imageSizePx, imageSizePx)
        button.setCompoundDrawablesRelative(null, image, null, null)
        button.setOnTouchListener(applyButtonPressEffect())
        return button
    }

    private fun applyButtonPressEffect() : View.OnTouchListener {
        return View.OnTouchListener { it, event ->
            val button : Button = it as Button
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    button.setTextColor(Color.parseColor("#FF6000"))
                    button.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#FF6000"))
                    Handler().postDelayed({
                        button.setTextColor(Color.BLACK)
                        button.compoundDrawableTintList = ColorStateList.valueOf(Color.BLACK)
                    }, DEVICE_BUTTON_HIGHLIGHT_LENGTH)
                }
            }
            return@OnTouchListener false
        }
    }

    override fun getItemCount(): Int {
        return roomData.size
    }
}

class RoomViewHolder(val containerView: View) : RecyclerView.ViewHolder(containerView)