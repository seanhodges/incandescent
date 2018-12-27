package uk.co.seanhodges.incandescent.client.selection

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.*
import uk.co.seanhodges.incandescent.client.scene.ApplySceneTask
import uk.co.seanhodges.incandescent.client.scene.DeleteSceneTask
import uk.co.seanhodges.incandescent.client.storage.DeviceViewMode
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices
import uk.co.seanhodges.incandescent.client.storage.SceneWithActions

private const val VIEW_TYPE_SCENE = 0
private const val VIEW_TYPE_GRID = 1
private const val VIEW_TYPE_LIST = 2

class ContentAdapter(
        private val launch: LaunchActivity = Inject.launch,
        private val executor: OperationExecutor = Inject.executor,
        private var activeOnly: Boolean = false
) : RecyclerView.Adapter<SectionViewHolder>() {

    private var sceneData: List<SceneWithActions> = emptyList()
    private var roomData: List<RoomWithDevices> = emptyList()
    private lateinit var parentView: ViewGroup
    private var viewMode = DeviceViewMode.GRID

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

    fun setViewMode(viewMode: DeviceViewMode) {
        this.viewMode = viewMode
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) : Int = when(position) {
        0 -> VIEW_TYPE_SCENE
        else -> if (viewMode == DeviceViewMode.GRID) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        this.parentView = parent
        val view = when(viewType) {
            VIEW_TYPE_SCENE -> LayoutInflater.from(parent.context).inflate(R.layout.content_scene_entry, parent, false)
            VIEW_TYPE_GRID -> LayoutInflater.from(parent.context).inflate(R.layout.content_select_grid_section, parent, false)
            else -> LayoutInflater.from(parent.context).inflate(R.layout.content_select_list_section, parent, false)
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
            val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_select_grid_entry, this.parentView, false) as Button
            val item = ListEntryDecorator(button, this.parentView)
                    .title(sceneWithActions.scene?.title ?: "")
                    .type("scene")
                    .build()
            buttonList.addView(item)
            item.isLongClickable = true
            item.isClickable = true
            item.setOnClickListener {
                ApplySceneTask(this.parentView.context, Inject.executor)
                        .execute(sceneWithActions.scene?.id!!)
            }
            item.setOnLongClickListener {
                val builder = AlertDialog.Builder(this.parentView.context, android.R.style.Theme_Material_Dialog_Alert)
                builder.setTitle(this.parentView.resources.getString(R.string.alert_title_delete_scene))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setMessage(this.parentView.resources.getString(R.string.alert_message_delete_scene))
                        .setNegativeButton(android.R.string.no) { dialog, _ ->
                            dialog.cancel()
                        }
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            DeleteSceneTask(this.parentView.context).execute(sceneWithActions.scene?.id)
                        }
                        .show()
                false
            }
        }

        val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_select_grid_entry, this.parentView, false) as Button
        val item = ListEntryDecorator(button, this.parentView)
                .title(parentView.resources.getString(R.string.add_new_scene_label))
                .type("add")
                .build()
        buttonList.addView(item)
        item.setOnClickListener {
            launch.addScene(this.parentView.context)
        }
    }

    private fun renderDevices(holder: SectionViewHolder, position: Int) {
        val room = roomData[position].room!!
        val roomImage : ImageView = holder.containerView.findViewById(R.id.room_image)
        roomImage.setImageResource(IconResolver.getRoomImage(room.title))
        val roomTitle : TextView = holder.containerView.findViewById(R.id.room_title)
        roomTitle.text = room.title

        val buttonList : ViewGroup = holder.containerView.findViewById(R.id.device_list)
        buttonList.removeAllViewsInLayout()

        roomData[position].getDevicesInOrder().forEach { device ->
            if (activeOnly && device.lastPowerValue == 0) {
                return@forEach
            }

            if (viewMode == DeviceViewMode.GRID) {
                val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_select_grid_entry, this.parentView, false) as Button
                val item = ListEntryDecorator(button, this.parentView)
                        .title(device.title)
                        .type(device.type)
                        .active(device.lastPowerValue == 1)
                        .build()
                item.setOnClickListener {
                    launch.deviceControl(this.parentView.context, room, device)
                }
                buttonList.addView(item)
            }
            else {
                val row: RelativeLayout = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_select_list_entry, this.parentView, false) as RelativeLayout
                row.findViewById<CheckBox>(R.id.action_pick).visibility = View.GONE

                val deviceImage: ImageView = row.findViewById(R.id.device_image)
                val actionToggle: Switch = row.findViewById(R.id.action_enable)
                actionToggle.visibility = View.VISIBLE
                deviceImage.setImageResource(IconResolver.getDeviceImage(device.title, device.type))
                if (device.lastPowerValue == 1) {
                    deviceImage.imageTintList = ColorStateList.valueOf(Color.parseColor(ENTRY_ACTIVE_COLOUR))
                    actionToggle.isChecked = true
                }
                row.findViewById<TextView>(R.id.device_name).text = device.title
                val controlButton = row.findViewById<ImageButton>(R.id.control_button)
                controlButton.setOnClickListener {
                    launch.deviceControl(this.parentView.context, room, device)
                }
                controlButton.setOnTouchListener(applyControlButtonPressEffect(controlButton))
                actionToggle.setOnClickListener {
                    device.powerCommand?.let { cmd ->
                        executor.enqueueChange(cmd, if (actionToggle.isChecked) 1 else 0)
                    }
                }
                buttonList.addView(row)
            }
        }

        // If there are no devices, collapse the row
        setVisibility(if (buttonList.childCount == 0) View.GONE else View.VISIBLE, roomImage, roomTitle, buttonList)
    }

    private fun applyControlButtonPressEffect(button: ImageButton) : View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setButtonColour(button, ENTRY_SELECTED_COLOUR)
                    Handler().postDelayed({
                        setButtonColour(button, ENTRY_DEFAULT_COLOUR)
                    }, DEVICE_BUTTON_HIGHLIGHT_LENGTH)
                }
            }
            return@OnTouchListener false
        }
    }

    private fun setButtonColour(button: ImageButton, colour: String) {
        button.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colour))
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