package uk.co.seanhodges.incandescent.client

import org.hamcrest.CoreMatchers.*
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import uk.co.seanhodges.incandescent.lightwave.event.*
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

@RunWith(MockitoJUnitRunner::class)
class EventsTest : DeviceChangeAware {

    private var called: Boolean = false
    private var resultFeatureId: String? = null
    private var resultNewValue: Int = -1

    private var server = mock(LightwaveServer::class.java)
    val eventHandler = DeviceChangeHandler(server)

    init {
        eventHandler.addListener(this)
    }

    @Before
    fun setUp() {
        called = false
    }

    override fun onDeviceChanged(featureId: String, newValue: Int) {
        called = true
        resultFeatureId = featureId
        resultNewValue = newValue
    }

    @Test
    fun itHandlesAFeatureEvent() {
        val payload = LWEventPayloadFeature(50, "success")
        payload.featureId = "FEATURE1"
        eventHandler.onEvent(anEvent(payload, "feature"))

        assertTrue(called)
        assertThat(resultFeatureId, equalTo("FEATURE1"))
        assertThat(resultNewValue, equalTo(50))
    }

    @Test
    fun itIgnoresAGroupEvent() {
        val payload = LWEventPayloadGroup(emptyMap())
        eventHandler.onEvent(anEvent(payload, "group"))

        assertFalse(called)
    }

    private fun anEvent(item: LWEventPayload, clazz: String) = LWEvent(
            1,
            "123",
            1,
            "event",
            "request",
            mutableListOf(LWEventItem(1, item)),
            clazz
    )
}