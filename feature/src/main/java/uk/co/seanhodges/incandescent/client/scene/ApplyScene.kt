package uk.co.seanhodges.incandescent.client.scene

import android.content.Context
import android.os.AsyncTask
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.SceneDao

class ApplyScene(
        ctx: Context,
        private val executor: OperationExecutor = Inject.executor
) : AsyncTask<Int, Void, Void>() {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(ctx).sceneDao()

    override fun doInBackground(vararg params: Int?): Void? {
        if (params.isEmpty()) return null
        val sceneId = params[0]!!
        val scene = sceneDao.findSceneById(sceneId)
        scene.actions?.forEach { action ->
            executor.enqueueChange(action.id, action.value)
        }
        return null
    }
}