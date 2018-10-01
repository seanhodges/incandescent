package uk.co.seanhodges.incandescent.lightwave.server

import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import junit.framework.TestCase.*
import org.junit.Ignore
import org.junit.Test
import uk.co.seanhodges.incandescent.lightwave.event.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGetRootGroups
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGroup

class LightwaveServerTest {

    private val server: LightwaveServer = LightwaveServer()

    @Test
    @Ignore
    @Throws(IOException::class)
    fun testAuthenticatesAndGetsAccessToken() {
        val accessToken : String = server.authenticate(LW_ACCOUNT_USERNAME, LW_ACCOUNT_PASSWORD)
        println(accessToken)
    }

    @Test
    @Ignore
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
        server.connect(ACCESS_TOKEN)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(connected.get())
    }

    @Test
    @Ignore
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
        server.connect(ACCESS_TOKEN)
        Thread.sleep(3000)
        val operation = LWOperation("user", "rootGroups")
        operation.addPayload(LWOperationPayloadGetRootGroups())
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals("[5b8aa9b4d36c330fd5b4e100-5b8aa9b4d36c330fd5b4e101]", result.toString())
    }

    @Test
    @Ignore
    @Throws(InterruptedException::class)
    fun testReadsGroupInfo() {
        val responded = AtomicBoolean(false)
        val result = StringBuilder()

        val listener = object : LWEventListener {
            override fun onEvent(event: LWEvent) {
                println(event)
                if (event.clazz == "group" && event.operation == "read") {
                    responded.set(true)
                    result.append((event.items[0].payload as LWEventPayloadGroup).features)
                }
            }

            override fun onError(e: Throwable) {
                throw RuntimeException(e)
            }
        }
        server.addListener(listener)
        server.connect(ACCESS_TOKEN)
        Thread.sleep(3000)
        val operation = LWOperation("group", "read")
        operation.addPayload(LWOperationPayloadGroup(ROOT_GROUP_ID))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals("9", result.toString())
    }

    @Test
    @Ignore
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
        server.connect(ACCESS_TOKEN)
        Thread.sleep(3000)
        val operation = LWOperation("group", "hierarchy")
        operation.addPayload(LWOperationPayloadGroup(ROOT_GROUP_ID))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals("9", result.toString())
    }

    @Test
    @Ignore
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
        server.connect(ACCESS_TOKEN)
        Thread.sleep(3000)
        val operation = LWOperation("feature", "read")
        operation.addPayload(LWOperationPayloadFeature(FEATURE_ID))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals(FEATURE_DIM_VALUE, result.get())
    }

    @Test
    @Ignore
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
        server.connect(ACCESS_TOKEN)
        Thread.sleep(3000)
        val operation = LWOperation("feature", "write")
        operation.addPayload(LWOperationPayloadFeature(FEATURE_ID, FEATURE_DIM_VALUE))
        server.command(operation)
        Thread.sleep(3000)
        server.disconnect()

        assertTrue(responded.get())
        assertEquals(FEATURE_DIM_VALUE, result.get())
    }

    companion object {
        private const val LW_ACCOUNT_USERNAME = "<email>"
        private const val LW_ACCOUNT_PASSWORD = "<password>"
        private const val ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiIzOWVmZjRiYzBiYzQyOTQ5ZGUxMyIsImlzcyI6Imh0dHBzOi8vYXV0aC5saWdodHdhdmVyZi5jb20iLCJzdWIiOiJjNTJiNDA4MS00MTA5LTQ4MTctOTE1NS1jNmY1YjBmMjdlYWQiLCJhdWQiOiJiZGU5NDFkMy1jNmMyLTQ5OGQtYjVlYS0zN2FmNjIzOGQ4NzAiLCJleHAiOjE1Mzg4NzEwMTUsImlhdCI6MTUzODI2NjIxNSwic2NvcGUiOiJsd2FwcHMifQ.uLUC4rdWsKEGUt16LTg1-Med-SvRv3F5e0pF6aFm-7EcuxEJVKQ36aSNCqpzLgB8OCL0gpMBbHXxPsA7bHSgch3TZaHVe7CiuYfXFagqGYfKlNSk_Xy3XNraZQRy1rdPXygs0QmopFTnbE58t6X7eIYf-ygFs2r8uvGZCEr0twOJ7D80N47dcPwMReEC13Ckrya2fDkcZ_0oeyKC41QYGUQNKv5TZE4fP6WkDVoQQnbVHvWVyafp-siF1MvJh647umtvJKMOi9remg9Y-UT0QO5gGhOWjDlUjlXodcFWSu2yx65YSzxxUYYs6KVgqmtjRdJKfbTi-1LYyqfl_NMwR4BCfIQw2Csz5xCaKVh_e0MFPTt748qUYDl6K5-HmSZrVeyde_-WniZdiyWrACi0ZgnutSUDfi8W1CK_8XGi-q4zy-P8tc4c646BTj-o9kkczPPfkzpcF6GagQ7wsmrydfB-X7kgM2YMc3qLXGV1_-sXZouZUjZRckLVvRfF5g8bfXCLu9wswQPgOU7QFMGEq0b68aoegqELk5AggNs-m3igcxGvNt-cC5Rb8jS-C06u1gAGxkghubh2cvp08BkFFhlKKHV2RCYCfDGEE-6gve2TGoFtVDkdi5J_tpnAZOKzRanOOdCB2k1mbXQYFEtFJGM_5LvpCQTeVuyQSwY9w3w"
        private const val ROOT_GROUP_ID = "5b8aa9b4d36c330fd5b4e100-5b8aa9b4d36c330fd5b4e101"
        private const val FEATURE_ID = "5b8aa9b4d36c330fd5b4e100-23-3157332334+1"
        private const val FEATURE_DIM_VALUE = 20
    }

}
