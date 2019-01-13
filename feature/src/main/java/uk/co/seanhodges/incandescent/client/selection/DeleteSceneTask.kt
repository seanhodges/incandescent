package uk.co.seanhodges.incandescent.client.selection

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.SceneDao
import java.lang.ref.WeakReference

class DeleteSceneTask(
        ctx: Context
) : AsyncTask<Long, Void, String>() {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(ctx).sceneDao()
    private val ctxRef = WeakReference(ctx)

    override fun doInBackground(vararg params: Long?): String? {
        if (params.isEmpty()) return null
        val sceneId = params[0]!!
        val sceneWithActions = sceneDao.findSceneById(sceneId)
        val scene = sceneWithActions.scene
        sceneDao.delete(scene, sceneWithActions.actions)
        Log.d(javaClass.name, "Deleted scene: ${scene?.title}")
        return scene?.title
    }

    override fun onPostExecute(title: String?) {
        if (!title.isNullOrEmpty() && ctxRef.get() != null) {
            Toast.makeText(ctxRef.get(), "$title was deleted", Toast.LENGTH_SHORT).show()
        }
    }
}