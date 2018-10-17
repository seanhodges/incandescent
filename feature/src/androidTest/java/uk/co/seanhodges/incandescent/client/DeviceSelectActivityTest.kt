package uk.co.seanhodges.incandescent.client

import android.support.test.espresso.Espresso.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.MediumTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule
import uk.co.seanhodges.incandescent.client.selection.DeviceSelectActivity


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DeviceSelectActivityTest {

    @Rule
    var rule: ActivityTestRule<DeviceSelectActivity> = ActivityTestRule(DeviceSelectActivity::class.java)

    @Test
    fun itShowsAListOfRoomsWithDeviceButtons() {
        onView(allOf(withId(R.id.deviceList), isDescendantOfA(withId(R.id.roomList))))
                .check(matches(isDisplayed()))
    }

}
