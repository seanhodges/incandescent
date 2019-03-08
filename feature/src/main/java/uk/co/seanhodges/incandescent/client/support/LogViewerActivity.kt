package uk.co.seanhodges.incandescent.client.support

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import uk.co.seanhodges.incandescent.client.R
import java.io.BufferedReader

const val LOGCAT_CMD = "logcat *:D -v time -b main -d | grep -E '[VDIWEF]/uk.co.seanhodges.incandescent.(client|lightwave)'"

class LogViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        val logView = findViewById<TextView>(R.id.logView)
        logView.setMovementMethod(ScrollingMovementMethod())
        val process = Runtime.getRuntime().exec(wrapCmd(LOGCAT_CMD))

        logView.text = process.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    private fun wrapCmd(cmd: String): Array<String> {
        return arrayOf("/system/bin/sh", "-c", cmd)
    }

    public override fun onDestroy() {
        super.onDestroy()
    }
}
