package uk.co.seanhodges.incandescent.client.support

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.lang.ref.WeakReference
import java.util.ArrayList

private const val EMAIL_RECIPIENT = "seanhodges84@gmail.com"
private const val EMAIL_SUBJECT = "Incandescent: Device incompatibility report"
private const val FILE_PROVIDER_AUTHORITY = "uk.co.seanhodges.incandescent.reports"

private const val REPORT_GROUP_HIERARCHY_FILENAME = "group_heirarchy_report.txt"
private const val REPORT_GROUP_READ_FILENAME = "group_read_report.txt"
private const val REPORT_FEATURE_READ_FILENAME = "feature_read_report.txt"

fun reportFilePath(ctx: Context, filename: String): File? = File(ctx.filesDir, "reports/$filename")
fun uriOf(ctx: Context, file: File?): Uri = FileProvider.getUriForFile(ctx, FILE_PROVIDER_AUTHORITY, file!!)

        class GatherDeviceReport(ctx: Context) {
    private val ctxRef = WeakReference<Context>(ctx)

    fun saveReport(packet: String) {
        if (packet.isEmpty()) {
            return
        }
        else if (packet.contains("\"class\":\"group\"") && packet.contains("\"operation\":\"hierarchy\"")) {
            saveToFile(REPORT_GROUP_HIERARCHY_FILENAME, packet)
        }
        else if (packet.contains("\"class\":\"group\"") && packet.contains("\"operation\":\"read\"")) {
            saveToFile(REPORT_GROUP_READ_FILENAME, packet)
        }
        else if (packet.contains("\"class\":\"feature\"") && packet.contains("\"operation\":\"read\"")) {
            saveToFile(REPORT_FEATURE_READ_FILENAME, packet)
        }
    }

    private fun saveToFile(filename: String, content: String) {
        ctxRef.get()?.let { ctx ->
            val file = reportFilePath(ctx, filename)
            file?.parentFile?.mkdirs()
            FileWriter(file).also { fw ->
                fw.write(content)
                fw.flush()
                fw.close()
            }
        }
    }
}

class SendDeviceReport(ctx: Context) {
    private val ctxRef = WeakReference<Context>(ctx)

    fun send(body: String) {
        val email = buildEmail(body)
        sendEmail(email)
    }

    private fun buildEmail(body: String) = Intent(ACTION_SEND_MULTIPLE).also { intent ->
        intent.type = "text/plain"
        intent.putExtra(EXTRA_EMAIL, arrayOf(EMAIL_RECIPIENT))
        intent.putExtra(EXTRA_SUBJECT, EMAIL_SUBJECT)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        ctxRef.get()?.let { ctx ->
            val uris = ArrayList<Parcelable>()
            uris.add(uriOf(ctx, reportFilePath(ctx, REPORT_GROUP_HIERARCHY_FILENAME)))
            uris.add(uriOf(ctx, reportFilePath(ctx, REPORT_GROUP_READ_FILENAME)))
            uris.add(uriOf(ctx, reportFilePath(ctx, REPORT_FEATURE_READ_FILENAME)))
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }

    private fun sendEmail(email: Intent) {
        try {
            ctxRef.get()?.startActivity(Intent.createChooser(email, "Send by email..."))
        }
        catch (e: Throwable) {
            Toast.makeText(ctxRef.get(), "Could not send email, no email apps installed.", Toast.LENGTH_SHORT).show()
        }
    }
}