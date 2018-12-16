package uk.co.seanhodges.incandescent.client.scene

import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.DeviceChangeHandler
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.storage.*


class AddSceneViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).sceneDao()
    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()
    private val deviceChangeHandler: DeviceChangeHandler = Inject.deviceChangeHandler

    fun listenForValueChanges(owner: LifecycleOwner) {
        deviceChangeHandler.setRepository(owner, deviceDao)
    }

    fun getAllRooms(): LiveData<List<RoomWithDevices>> {
        return roomDao.loadAllWithDevices()
    }

    fun save(name: String, settings: List<FlatDeviceRow>) {
        SaveSceneTask(sceneDao).execute(AddSceneForm(name, settings))
    }

    private class SaveSceneTask(
            private val sceneDao: SceneDao
    ) : AsyncTask<AddSceneForm, Void, Void>() {

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
}

data class AddSceneForm (
        val name: String,
        val settings: List<FlatDeviceRow>
)
