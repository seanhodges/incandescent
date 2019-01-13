package uk.co.seanhodges.incandescent.client

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Rule
import uk.co.seanhodges.incandescent.client.selection.DeviceSelectActivity


/**
 * Instrumented test, which will execute on an Android appliance.
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
        onView(allOf(withId(R.id.device_list), isDescendantOfA(withId(R.id.room_list))))
                .check(matches(isDisplayed()))
    }

}
