package uk.co.seanhodges.incandescent.client

import android.content.Context
import android.content.Intent
import uk.co.seanhodges.incandescent.client.auth.AuthenticateActivity
import uk.co.seanhodges.incandescent.client.control.DeviceControlActivity
import uk.co.seanhodges.incandescent.client.scene.AddSceneActivity
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import uk.co.seanhodges.incandescent.client.support.LogViewerActivity

class LaunchActivity() {

    fun authenticate(context: Context) {
        context.startActivity(Intent(context, AuthenticateActivity::class.java))
    }

    fun deviceControl(context: Context, room: RoomEntity?, device: DeviceEntity?) {
        val intent = Intent(context, DeviceControlActivity::class.java)
        intent.putExtra("selectedRoom", room)
        intent.putExtra("selectedDevice", device)
        context.startActivity(intent)
    }

    fun addScene(context: Context) {
        context.startActivity(Intent(context, AddSceneActivity::class.java))
    }

    fun showLog(context: Context) {
        context.startActivity(Intent(context, LogViewerActivity::class.java))
    }

}