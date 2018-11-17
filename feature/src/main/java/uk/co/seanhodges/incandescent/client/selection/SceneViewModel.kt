package uk.co.seanhodges.incandescent.client.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.storage.*


class SceneViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).sceneDao()
    private val scenes: LiveData<List<SceneWithActions>> = sceneDao.loadAllWithActions()

    fun getAllScenes(): LiveData<List<SceneWithActions>> {
        return scenes
    }

}