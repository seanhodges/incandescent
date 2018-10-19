package uk.co.seanhodges.incandescent.lightwave.server

import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import junit.framework.TestCase.*
import org.junit.Test
import uk.co.seanhodges.incandescent.lightwave.event.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGetRootGroups
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGroup

class LightwaveServerIntTest {

    private val server: LightwaveServer = LightwaveServer()

    @Test
    @Throws(IOException::class)
    fun testAuthenticatesAndGetsAccessToken() {
        val username = System.getProperty("LW_ACCOUNT_USERNAME")!!
        val password = System.getProperty("LW_ACCOUNT_PASSWORD")!!
        val result : LWAuthenticatedResult = server.authenticate(username, password)
        println(result.tokens.accessToken)
    }

    private fun authenticate(): String {
        val username = System.getProperty("LW_ACCOUNT_USERNAME")!!
        val password = System.getProperty("LW_ACCOUNT_PASSWORD")!!
        val result : LWAuthenticatedResult = server.authenticate(username, password)
        return result.tokens.accessToken
    }

    @Test
    @Throws(InterruptedException::class)
    fun testConnectsToWS() {
        val connected = AtomicBoolean(false)

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                connected.set(true)
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(connected.get())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testGetsRootGroups() {
        val responded = AtomicBoolean(false)
        val result = StringBuilder()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                if (event.clazz == "user" && event.operation == "rootGroups") {
                    responded.set(true)
                    result.append((event.items[0].payload as LWEventPayloadGetRootGroups).groupIds)
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        val operation = LWOperation("user", "", "rootGroups")
        operation.addPayload(LWOperationPayloadGetRootGroups())
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals("[5b8aa9b4d36c330fd5b4e100-5b8aa9b4d36c330fd5b4e101]", result.toString())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testReadsGroupInfo() {
        val responded = AtomicBoolean(false)
        var result = mapOf<String, LWEventPayloadGroupFeature>()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                println(event)
                if (event.clazz == "group" && event.operation == "read") {
                    responded.set(true)
                    result = (event.items[0].payload as LWEventPayloadGroup).features
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        val operation = LWOperation("group", "", "read")
        operation.addPayload(LWOperationPayloadGroup(ROOT_GROUP_ID))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals(57, result.size)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testReadsGroupHierarchy() {
        val responded = AtomicBoolean(false)
        val result = StringBuilder()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                println(event)
                if (event.clazz == "group" && event.operation == "hierarchy") {
                    responded.set(true)
                    //result.append((event.items[0].payload as LWEventPayloadGroup).features)
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        val operation = LWOperation("group", "", "hierarchy")
        operation.addPayload(LWOperationPayloadGroup(ROOT_GROUP_ID))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals("9", result.toString())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testReadsDimSetting() {
        val responded = AtomicBoolean(false)
        val result = AtomicInteger()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                if (event.clazz == "feature" && event.operation == "read") {
                    responded.set(true)
                    result.set((event.items[0].payload as LWEventPayloadFeature).value)
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        val operation = LWOperation("feature", "", "read")
        operation.addPayload(LWOperationPayloadFeature(FEATURE_ID))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals(FEATURE_DIM_VALUE, result.get())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testChangesDimSetting() {
        val responded = AtomicBoolean(false)
        val result = AtomicInteger()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                if (event.clazz == "feature" && event.operation == "write") {
                    responded.set(true)
                    result.set((event.items[0].payload as LWEventPayloadFeature).value)
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        val operation = LWOperation("feature", "", "write")
        operation.addPayload(LWOperationPayloadFeature(FEATURE_ID, FEATURE_DIM_VALUE))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals(FEATURE_DIM_VALUE, result.get())
    }


    @Test
    @Throws(InterruptedException::class)
    fun testReadsIdentify() {
        val responded = AtomicBoolean(false)
        val result = AtomicInteger()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                if (event.clazz == "feature" && event.operation == "read") {
                    responded.set(true)
                    result.set((event.items[0].payload as LWEventPayloadFeature).value)
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(authenticate(), "")
        Thread.sleep(3000)
        val operation = LWOperation("feature", "", "read")
        operation.addPayload(LWOperationPayloadFeature("5b8aa9b4d36c330fd5b4e100-40-3157332334+1"))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals(99, result.get())
    }

    companion object {
        private const val ROOT_GROUP_ID = "5b8aa9b4d36c330fd5b4e100-5b8aa9b4d36c330fd5b4e101"
        private const val FEATURE_ID = "5b8aa9b4d36c330fd5b4e100-23-3157332334+1"
        private const val FEATURE_DIM_VALUE = 20
    }

}
