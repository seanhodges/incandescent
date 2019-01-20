package uk.co.seanhodges.incandescent.client.fragment.sceneList

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import uk.co.seanhodges.incandescent.client.IconResolver
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LaunchActivity
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.fragment.SectionViewHolder
import uk.co.seanhodges.incandescent.client.storage.SceneEntity
import uk.co.seanhodges.incandescent.client.storage.SceneWithActions

class SceneContentAdapter(
        private val launch: LaunchActivity = Inject.launch,
        var theme: Theme = Theme.Light // TODO: Move this to a material theme
) : RecyclerView.Adapter<SectionViewHolder>() {

    private var sceneData: MutableList<FlatSceneRow> = mutableListOf()
    private lateinit var parentView: ViewGroup

    enum class Theme {
        Light,
        Dark
    }

    fun setSceneData(newData: List<SceneWithActions>, enabled: List<String> = emptyList()) {
        this.sceneData.clear()
        newData.forEach { scene ->
            val entry = FlatSceneRow(buildLabel(scene.scene!!), scene.scene!!)
            if (enabled.contains(scene.scene?.title)) entry.enabled = true
            this.sceneData.add(entry)
        }
        notifyDataSetChanged()
    }

    private fun buildLabel(scene: SceneEntity): String = scene.title

    fun getEnabledSceneData(): List<FlatSceneRow> {
        return sceneData.filter { it.enabled }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        this.parentView = parent
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_select_list_entry, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val rowData = sceneData[position]

        val row = holder.containerView
        row.findViewById<Switch>(R.id.action_enable).visibility = View.GONE

        val deviceImage: ImageView = row.findViewById(R.id.device_image)
        applyThemeGraphic(deviceImage)
        deviceImage.setImageResource(IconResolver.getDeviceImage(rowData.title, "scene"))

        val title : TextView = holder.containerView.findViewById(R.id.device_name)
        applyThemeText(title)
        title.text = rowData.title

        row.findViewById<ImageButton>(R.id.control_button).visibility = View.INVISIBLE

        val pick = row.findViewById<CheckBox>(R.id.action_pick)
        pick.visibility = View.VISIBLE
        pick.isChecked = rowData.enabled
        pick.setOnCheckedChangeListener { _, checked ->
            rowData.enabled = checked
        }
    }

    private fun applyThemeText(component: TextView) {
        if (theme == Theme.Dark) {
            component.setTextColor(ColorStateList.valueOf(parentView.resources.getColor(R.color.theme_light_list_fg_colour, null)))
        }
    }

    private fun applyThemeGraphic(component: ImageView) {
        if (theme == Theme.Dark) {
            component.imageTintList = ColorStateList.valueOf(parentView.resources.getColor(R.color.theme_light_list_fg_colour, null))
            component.imageTintMode = PorterDuff.Mode.SRC_IN
            component.backgroundTintList = ColorStateList.valueOf(parentView.resources.getColor(R.color.theme_light_list_fg_colour, null))
            component.backgroundTintMode = PorterDuff.Mode.SRC_IN
        }
    }

    override fun getItemCount(): Int {
        return sceneData.size
    }
}

data class FlatSceneRow(
        val title: String,
        val scene: SceneEntity
) {
    var enabled: Boolean = false
}