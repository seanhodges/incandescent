package uk.co.seanhodges.incandescent.client.scene

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.SceneDao

class ApplyScene(
        ctx: Context,
        private val executor: OperationExecutor = Inject.executor
) : AsyncTask<Long, Void, Void>() {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(ctx).sceneDao()

    override fun doInBackground(vararg params: Long?): Void? {
        if (params.isEmpty()) return null
        val sceneId = params[0]!!
        val scene = sceneDao.findSceneById(sceneId)
        Log.d(javaClass.name, "Found ${scene.actions?.size} actions")
        scene.actions?.forEach { action ->
            Log.d(javaClass.name, "Applying: ${action.id} = ${action.value}")
            executor.enqueueChange(action.id, action.value)
        }
        return null
    }
}