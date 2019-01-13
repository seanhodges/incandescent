package uk.co.seanhodges.incandescent.client.scene

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import uk.co.seanhodges.incandescent.client.fragment.applianceList.FlatDeviceRow
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.SceneActionEntity
import uk.co.seanhodges.incandescent.client.storage.SceneDao
import uk.co.seanhodges.incandescent.client.storage.SceneEntity

class SaveSceneTask(
        ctx: Context
) : AsyncTask<AddSceneForm, Void, Void>() {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(ctx).sceneDao()

    override fun doInBackground(vararg params: AddSceneForm): Void? {
        val addSceneForm = params[0]
        val scene = SceneEntity(addSceneForm.name)
        val actions = mutableListOf<SceneActionEntity>()
        Log.d(javaClass.name, "Saving scene as ${addSceneForm.name}")
        addSceneForm.settings.forEach {setting ->
            setting.device.powerCommand?.apply {
                Log.d(javaClass.name, "Saving ${this} as ${setting.device.lastPowerValue}")
                actions.add(SceneActionEntity(this, setting.device.lastPowerValue))
            }
            setting.device.dimCommand?.apply {
                Log.d(javaClass.name, "Saving ${this} as ${setting.device.lastDimValue}")
                actions.add(SceneActionEntity(this, setting.device.lastDimValue))
            }
        }
        sceneDao.insertSceneWithActions(scene, actions)
        return null
    }
}

data class AddSceneForm (
        val name: String,
        val settings: List<FlatDeviceRow>
)