package uk.co.seanhodges.incandescent.client.selection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import uk.co.seanhodges.incandescent.client.IconResolver
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.DeviceListSize
import uk.co.seanhodges.incandescent.client.storage.SettingsRepository
import java.lang.ref.WeakReference

private const val DEVICE_BUTTON_HIGHLIGHT_LENGTH : Long = 300

class ListEntryDecorator(private val button: Button, private val parent: ViewGroup) {

    private var title: String = ""
    private var type: String = ""

    fun title(title: String) = apply {
        this.title = title
    }

    fun type(type: String) = apply {
        this.type = type
    }

    @SuppressLint("ClickableViewAccessibility")
    fun build(): Button {
        val settingsRepository = SettingsRepository(WeakReference(parent.context))
        val settings = settingsRepository.get()
        button.text = title
        button.textSize = getButtonTextSize(settings.deviceListSize)
        button.width = getButtonSize(settings.deviceListSize)
        val image = parent.resources.getDrawable(IconResolver.getDeviceImage(title, type), null)
        val imageSize = getButtonImageSize(settings.deviceListSize)
        image.setBounds(0, 0, imageSize, imageSize)
        button.setCompoundDrawablesRelative(null, image, null, null)
        button.setOnTouchListener(applyButtonPressEffect())
        return button
    }

    private fun getButtonSize(deviceListSize: DeviceListSize): Int {
        val dim : Int = when(deviceListSize) {
            DeviceListSize.SMALL -> R.dimen.select_device_button_size_small
            else -> R.dimen.select_device_button_size_large
        }
        return parent.resources.getDimension(dim).toInt()
    }

    private fun getButtonImageSize(deviceListSize: DeviceListSize): Int {
        val dim : Int = when(deviceListSize) {
            DeviceListSize.SMALL -> R.dimen.select_device_image_size_small
            else -> R.dimen.select_device_image_size_large
        }
        return parent.resources.getDimension(dim).toInt()
    }

    private fun getButtonTextSize(deviceListSize: DeviceListSize): Float {
        val dim : Int = when(deviceListSize) {
            DeviceListSize.SMALL -> R.dimen.select_device_text_size_small
            else -> R.dimen.select_device_text_size_large
        }
        return parent.resources.getDimension(dim) / parent.resources.displayMetrics.density
    }

    private fun applyButtonPressEffect() : View.OnTouchListener {
        return View.OnTouchListener { it, event ->
            val button : Button = it as Button
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    button.setTextColor(Color.parseColor("#FF6000"))
                    button.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#FF6000"))
                    Handler().postDelayed({
                        button.setTextColor(Color.BLACK)
                        button.compoundDrawableTintList = ColorStateList.valueOf(Color.BLACK)
                    }, DEVICE_BUTTON_HIGHLIGHT_LENGTH)
                }
            }
            return@OnTouchListener false
        }
    }
}