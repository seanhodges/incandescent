package uk.co.seanhodges.incandescent.client.support

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_report_device.*
import uk.co.seanhodges.incandescent.client.R
import java.lang.StringBuilder

class ReportDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_device)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            SendDeviceReport(this).send(buildBody())
            finish()
        }
    }

    private fun buildBody(): String {
        val name = findViewById<EditText>(R.id.report_device_name)
        val problem = findViewById<EditText>(R.id.report_device_problem)

        return StringBuilder().also {
            it.append("Name: ${name.text}\n\n")
            it.append("Problem: ${problem.text}\n\n")
        }.toString()
    }

}
