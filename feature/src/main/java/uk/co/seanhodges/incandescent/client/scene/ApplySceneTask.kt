package uk.co.seanhodges.incandescent.client.scene

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.OperationExecutor
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.SceneDao
import java.lang.ref.WeakReference

class ApplySceneTask(
        ctx: Context,
        private val executor: OperationExecutor = Inject.executor
) : AsyncTask<Long, Void, String>() {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(ctx).sceneDao()
    private val ctxRef = WeakReference(ctx)

    override fun doInBackground(vararg params: Long?): String? {
        if (params.isEmpty()) return null
        val sceneId = params[0]!!
        val scene = sceneDao.findSceneById(sceneId)
        Log.d(javaClass.name, "Found ${scene.actions?.size} actions")
        scene.actions?.forEach { action ->
            Log.d(javaClass.name, "Applying: ${action.id} = ${action.value}")
            executor.enqueueChange(action.id, action.value)
        }
        return scene.scene?.title
    }

    override fun onPostExecute(title: String?) {
        if (!title.isNullOrEmpty() && ctxRef.get() != null) {
            Toast.makeText(ctxRef.get(), "$title has been applied", Toast.LENGTH_SHORT).show()
        }
    }
}