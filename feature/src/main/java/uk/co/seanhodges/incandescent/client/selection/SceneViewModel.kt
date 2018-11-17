package uk.co.seanhodges.incandescent.client.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices


class SceneViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).roomDao()
    private val scenes: LiveData<List<Scene>> = sceneDao.loadAll()

    fun getAllScenes(): LiveData<List<RoomWithDevices>> {
        return scenes
    }

}