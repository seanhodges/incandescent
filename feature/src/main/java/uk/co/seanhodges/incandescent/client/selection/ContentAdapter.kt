package uk.co.seanhodges.incandescent.client.selection

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LaunchActivity
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import uk.co.seanhodges.incandescent.client.scene.AddSceneActivity
import uk.co.seanhodges.incandescent.client.scene.ApplySceneTask
import uk.co.seanhodges.incandescent.client.scene.DeleteSceneTask
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices
import uk.co.seanhodges.incandescent.client.storage.SceneWithActions

private const val VIEW_TYPE_SCENE = 0
private const val VIEW_TYPE_ROOM = 1

class ContentAdapter(
        private val launch: LaunchActivity = Inject.launch,
        private var activeOnly: Boolean = false
) : RecyclerView.Adapter<SectionViewHolder>() {

    private var sceneData: List<SceneWithActions> = emptyList()
    private var roomData: List<RoomWithDevices> = emptyList()
    private lateinit var parentView: ViewGroup

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

        val button: Button = LayoutInflater.from(this.parentView.context).inflate(R.layout.content_list_entry, this.parentView, false) as Button
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
                launch.deviceControl(this.parentView.context, room, device)
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