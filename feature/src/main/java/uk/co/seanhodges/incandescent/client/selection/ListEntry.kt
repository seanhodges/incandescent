package uk.co.seanhodges.incandescent.client.selection

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import uk.co.seanhodges.incandescent.client.IconResolver
import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.SettingsRepository
import java.lang.ref.WeakReference

private const val DEVICE_BUTTON_HIGHLIGHT_LENGTH : Long = 300

public const val ENTRY_DEFAULT_COLOUR = "#000000"
public const val ENTRY_SELECTED_COLOUR = "#00927C"
public const val ENTRY_ACTIVE_COLOUR = "#FF6000"

class ListEntryDecorator(private val button: Button, private val parent: ViewGroup) {

    private var title: String = ""
    private var type: String = ""
    private var active: Boolean = false

    fun title(title: String) = apply {
        this.title = title
    }

    fun type(type: String) = apply {
        this.type = type
    }

    fun active(active: Boolean) = apply {
        this.active = active
    }

    @SuppressLint("ClickableViewAccessibility")
    fun build(): Button {
        val settingsRepository = SettingsRepository(WeakReference(parent.context))
        val settings = settingsRepository.get()
        button.text = title
        button.textSize = getButtonTextSize()
        button.width = getButtonSize()
        val image = calculateImage()
        val imageSize = getButtonImageSize()
        image.setBounds(0, 0, imageSize, imageSize)
        button.setCompoundDrawablesRelative(null, image, null, null)
        button.setOnTouchListener(applyButtonPressEffect())
        if (active) {
            setButtonColour(ENTRY_ACTIVE_COLOUR)
        }
        return button
    }

    private fun calculateImage(): Drawable {
        if (type.equals("add")) {
            return parent.resources.getDrawable(R.drawable.entry_add_new, null)
        }

        return parent.resources.getDrawable(IconResolver.getDeviceImage(title, type), null)
    }

    private fun getButtonSize(): Int {
        val dim : Int = R.dimen.select_device_button_size_small
        return parent.resources.getDimension(dim).toInt()
    }

    private fun getButtonImageSize(): Int {
        val dim : Int = R.dimen.select_device_image_size_small
        return parent.resources.getDimension(dim).toInt()
    }

    private fun getButtonTextSize(): Float {
        val dim : Int = R.dimen.select_device_text_size_small
        return parent.resources.getDimension(dim) / parent.resources.displayMetrics.density
    }

    private fun applyButtonPressEffect() : View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setButtonColour(ENTRY_SELECTED_COLOUR)
                    Handler().postDelayed({
                        setButtonColour(if (active) ENTRY_ACTIVE_COLOUR else ENTRY_DEFAULT_COLOUR)
                    }, DEVICE_BUTTON_HIGHLIGHT_LENGTH)
                }
            }
            return@OnTouchListener false
        }
    }

    private fun setButtonColour(colour: String) {
        button.setTextColor(Color.parseColor(colour))
        button.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor(colour))
    }


}