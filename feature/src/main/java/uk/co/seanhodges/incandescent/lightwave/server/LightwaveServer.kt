package uk.co.seanhodges.incandescent.lightwave.server

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.*
import uk.co.seanhodges.incandescent.lightwave.event.LWEvent
import uk.co.seanhodges.incandescent.lightwave.event.LWEventListener
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.event.EventPayloadTypeAdapter
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadConnect
import uk.co.seanhodges.incandescent.lightwave.operation.OperationPayloadTypeAdapter

import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

private const val LIGHTWAVE_VERSION: String = "1.8.12"
private const val MAX_IDLE_CONNECTIONS: Int = 5
private const val KEEP_ALIVE_DURATION_MINS: Long = 30

class LightwaveServer : WebSocketListener() {

    private val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION_MINS, TimeUnit.MINUTES))
            .build()

    private val authenticatedAdapter: JsonAdapter<LWAuthenticatedResult>
    private val tokensAdapter: JsonAdapter<LWAuthenticatedTokens>
    private val operationAdapter: JsonAdapter<LWOperation>
    private val eventAdapter: JsonAdapter<LWEvent>

    private var webSocket: WebSocket? = null
    var socketActive: Boolean = false
    private var reconnectAttempted: Boolean = false

    private var accessToken: String? = null
    private var senderId: String = ""

    private val listeners = ArrayList<LWEventListener>()

    init {
        val moshi = Moshi.Builder()
                .add(OperationPayloadTypeAdapter())
                .add(EventPayloadTypeAdapter())
                .build()
        authenticatedAdapter = moshi.adapter(LWAuthenticatedResult::class.java)
        tokensAdapter = moshi.adapter(LWAuthenticatedTokens::class.java)
        operationAdapter = moshi.adapter(LWOperation::class.java)
        eventAdapter = moshi.adapter(LWEvent::class.java)
    }

    fun addListener(listener: LWEventListener) {
        listeners.add(listener)
    }

    @Throws(IOException::class)
    fun authenticate(username: String, password: String): LWAuthenticatedResult {
        val json = "{\"email\":\"$username\",\"password\":\"$password\",\"version\":\"$LIGHTWAVE_VERSION\"}"
        val body = RequestBody.create(JSON_CONTENT_TYPE, json)
        val req = Request.Builder()
                .url("https://auth.lightwaverf.com/v2/lightwaverf/autouserlogin/lwapps")
                .addHeader("x-lwrf-platform", "android")
                .addHeader("x-lwrf-appid", "android-01")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "LightwaveApp/107001 CFNetwork/901.1 Darwin/17.6.0")
                .addHeader("Content-Type", "application/json")
                .addHeader("Host", "auth.lightwaverf.com")
                .addHeader("Accept-Language", "en-us")
                .post(body)
                .build()
        val res = client.newCall(req).execute()
        val resultStr = res.body()!!.string()
        res.close()

        println("<<< $resultStr")
        return authenticatedAdapter.fromJson(resultStr)!!
    }

    fun refreshToken(refreshToken: String): LWAuthenticatedTokens  {
        val json = "{\"grant_type\":\"refresh_token\",\"refresh_token\":\"$refreshToken\",\"version\":\"$LIGHTWAVE_VERSION\"}"
        val body = RequestBody.create(JSON_CONTENT_TYPE, json)
        val req = Request.Builder()
                .url("https://auth.lightwaverf.com/v2/lightwaverf/autouserlogin/lwapps")
                .addHeader("x-lwrf-platform", "android")
                .addHeader("x-lwrf-appid", "android-01")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "LightwaveApp/107001 CFNetwork/901.1 Darwin/17.6.0")
                .addHeader("Content-Type", "application/json")
                .addHeader("Host", "auth.lightwaverf.com")
                .addHeader("Accept-Language", "en-us")
                .post(body)
                .build()
        val res = client.newCall(req).execute()
        val resultStr = res.body()!!.string()
        res.close()

        println("<<< $resultStr")
        return tokensAdapter.fromJson(resultStr)!!
    }

    fun connect(accessToken: String, senderId: String) {
        disconnect()
        this.accessToken = accessToken
        this.senderId = senderId
        val req = Request.Builder()
                .url("wss://v1-linkplus-app.lightwaverf.com")
                .build()
        webSocket = client.newWebSocket(req, this)
        //note(sean): see onOpen() for the second auth phase for websocket
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        val operation = LWOperation("user", senderId, "authenticate")
        operation.addPayload(LWOperationPayloadConnect(accessToken!!, senderId))
        command(operation)
        this.socketActive = true
        this.reconnectAttempted = false
    }

    fun command(command: LWOperation) {
        if (this.webSocket == null && this.socketActive) {
            // We lost websocket connection unexpectedly, retry and wait for connection to open
            connect(this.accessToken!!, this.senderId)
            Thread.sleep(3000)
        }

        val json = operationAdapter.toJson(command)
        println(">>> $json")
        this.webSocket!!.send(json)
    }

    fun disconnect() {
        if (webSocket != null) {
            webSocket!!.close(SOCKET_CLOSE_STATUS, "")
        }
        this.socketActive = false
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        println("<<< $text")

        try {
            for (listener in listeners) {
                listener.onRawEvent(text)
            }

            val event = eventAdapter.fromJson(text)
            event?.json = text

            for (listener in listeners) {
                listener.onEvent(event!!)
            }
        } catch (e: IOException) {
            this.onFailure(webSocket, e, null)
        }

    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        if (t is SSLException && t.message?.contains("Software caused connection abort") == true && !this.reconnectAttempted) {
            // Likely the device network state changed and okhttp could not recover, try and reconnect once more
            this.reconnectAttempted = true
            connect(this.accessToken!!, this.senderId)
            return
        }

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        t.printStackTrace(printWriter)

        println("[ERROR!] " + t.toString() + "\n" + writer.toString())
        for (listener in listeners) {
            listener.onError(t)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        this.webSocket = null
    }

    companion object {
        private val JSON_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8")
        private const val SOCKET_CLOSE_STATUS = 1000
    }
}

data class LWAuthenticatedResult(
        val _id : String,
        val givenName : String,
        val familyName : String,
        val tokens : LWAuthenticatedTokens
)

data class LWAuthenticatedTokens(
        @field:Json(name = "access_token") val accessToken : String,
        @field:Json(name = "token_type") val tokenType : String,
        @field:Json(name = "expires_in") val expiresIn : Long,
        @field:Json(name = "refresh_token") val refreshToken : String,
        @field:Json(name = "id_token") val idToken : String
)
