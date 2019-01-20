package uk.co.seanhodges.incandescent.client.receive

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import uk.co.seanhodges.incandescent.client.R
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractFragmentPluginActivity
import uk.co.seanhodges.incandescent.client.fragment.applianceList.ApplianceContentAdapter
import uk.co.seanhodges.incandescent.client.fragment.applianceList.ApplianceListViewModel
import uk.co.seanhodges.incandescent.client.storage.*


class MakeBundleActivity() : AbstractFragmentPluginActivity() {

    private lateinit var applianceViewModel: ApplianceListViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var contentAdapter: ApplianceContentAdapter

    private var previousCmd: CommandBundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_bundle)

        try {
            title = packageManager.getApplicationLabel(packageManager.getApplicationInfo(callingPackage, 0))
        } catch (e: Exception) {
            Log.e(javaClass.name, "Calling package couldn't be found", e)
        }

        contentAdapter = ApplianceContentAdapter(theme = ApplianceContentAdapter.Theme.Dark)
        recyclerView = this.findViewById(R.id.bundle_setting_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contentAdapter

        applianceViewModel = ViewModelProviders.of(this).get(ApplianceListViewModel::class.java)
        applianceViewModel.getAllRooms().observe(this, Observer<List<RoomWithDevices>> { roomsWithDevices ->
            val enabled = mutableListOf<String>()
            roomsWithDevices.map { room ->
                room.devices?.map { device ->
                    if (previousCmd?.appliances?.find { it.id == device.id } != null) {
                        enabled.add(device.id)
                    }
                }
            }

            contentAdapter.setDeviceData(roomsWithDevices, enabled)
        })

        actionBar?.setSubtitle(R.string.application_name)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPostCreateWithPreviousResult(previousBundle: Bundle, previousBlurb: String) {
        previousCmd = BundleUtils.unpackBundle(previousBundle)
    }

    override fun isBundleValid(bundle: Bundle): Boolean {
        return BundleUtils.isBundleValid(bundle)
    }

    override fun getResultBundle(): Bundle? {
        // TODO: Support scenes
        val scenes = emptyList<String>()

        val enabledDevices = contentAdapter.getEnabledDeviceData()
        val appliances = enabledDevices.map { ApplianceBundle(it) }

        // Generate the bundle object from the UI
        val cmd = CommandBundle(title.toString(), scenes, appliances)
        return BundleUtils.generateBundle(cmd)
    }

    override fun getResultBlurb(bundle: Bundle): String {
        val cmd = BundleUtils.unpackBundle(bundle)
        val blurb = buildLabel(cmd)

        val maxBlurbLength = resources.getInteger(
                R.integer.com_twofortyfouram_locale_sdk_client_maximum_blurb_length)
        if (blurb.length > maxBlurbLength) {
            // Truncate the start of the blurb
            return "..." + blurb.substring(blurb.length - maxBlurbLength - 4, blurb.length - 1)
        }
        return blurb
    }

    private fun buildLabel(cmd: CommandBundle): String {
        if (cmd.scenes.isNotEmpty()) {
            return buildLabel(cmd.scenes)
        }
        else if (cmd.appliances.size == 1) {
            return buildLabel(cmd.appliances[0])
        }
        return "Control ${cmd.appliances.size} appliances"
    }

    private fun buildLabel(scenes: List<String>): String = scenes.joinToString(
            prefix = "Scenes: ", separator = ", ")

    private fun buildLabel(appliance: ApplianceBundle): String =
            buildLabel(appliance.roomName, appliance.applianceName, appliance.power == 1, appliance.dim)

    private fun buildLabel(roomName: String?, applianceName: String?, power: Boolean?, dim: Int?): String {
        val status = when (power) {
            true -> if (dim ?: 0 > 0) "On, ${dim}%" else "On"
            else -> "Off"
        }
        return "${roomName} > ${applianceName} ($status)"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_make_bundle, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (android.R.id.home == item.itemId) {
            finish()
        }
        else if (R.id.menu_cancel == item.itemId) {
            mIsCancelled = true // Signal to AbstractAppCompatPluginActivity that the user cancelled.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}