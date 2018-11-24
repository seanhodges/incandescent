package uk.co.seanhodges.incandescent.client.scene

import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LastValueChangeListener
import uk.co.seanhodges.incandescent.client.storage.*


class AddSceneViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).sceneDao()
    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()
    private val roomsWithDevices: LiveData<List<RoomWithDevices>> = roomDao.loadAllWithDevices()
    private val lastValueChangeListener: LastValueChangeListener = Inject.lastValueChangeListener

    fun listenForValueChanges(owner: LifecycleOwner) {
        lastValueChangeListener.setRepository(owner, deviceDao)
    }

    fun getAllRooms(): LiveData<List<RoomWithDevices>> {
        return roomsWithDevices
    }

    fun save(name: String, settings: List<FlatDeviceRow>) {
        val actions = mutableListOf<SceneActionEntity>()
        settings.forEach {setting ->
            setting.device.powerCommand?.apply {
                actions.add(SceneActionEntity(this, setting.device.lastPowerValue))
            }
            setting.device.dimCommand?.apply {
                actions.add(SceneActionEntity(this, setting.device.lastDimValue))
            }
        }
        val scene = SceneEntity(name)
        sceneDao.insertSceneWithActions(scene, actions)
    }
}