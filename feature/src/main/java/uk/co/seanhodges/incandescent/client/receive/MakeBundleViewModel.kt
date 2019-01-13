package uk.co.seanhodges.incandescent.client.receive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.storage.*


class MakeBundleViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).sceneDao()
    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()

    fun getAllScenes(): LiveData<List<SceneWithActions>> {
        return sceneDao.loadAllWithActions()
    }

    fun getAllRooms(): LiveData<List<RoomWithDevices>> {
        return roomDao.loadAllWithDevices()
    }
}