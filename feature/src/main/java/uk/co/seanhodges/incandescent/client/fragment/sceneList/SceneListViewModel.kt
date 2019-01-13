package uk.co.seanhodges.incandescent.client.fragment.sceneList

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.storage.*


class SceneListViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).sceneDao()

    fun getAllScenes(): LiveData<List<SceneWithActions>> {
        return sceneDao.loadAllWithActions()
    }
}