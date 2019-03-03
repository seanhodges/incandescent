package uk.co.seanhodges.incandescent.client.selection

import android.util.Log
import com.squareup.moshi.JsonReader
import okio.Buffer
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import uk.co.seanhodges.incandescent.lightwave.event.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGetRootGroups
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

class LightwaveConfigLoader(
        private val server: LightwaveServer
) : LWEventListener {

    private lateinit var onComplete: (hierarchy: String, info: LWEventPayloadGroup) -> Unit

    private var groupHierarchy: String? = null
    private var groupInfo: LWEventPayloadGroup? = null

    init {
        server.addListener(this)
    }

    override fun onEvent(event: LWEvent) {
        if (event.clazz.equals("user") && event.operation.equals("authenticate")) {
            getRootGroupId()
        }
        else if (event.clazz == "user" && event.operation == "rootGroups") {
            // Root group ID loaded, fetch the group data
            val payload = event.items[0].payload as LWEventPayloadGetRootGroups
            val rootGroupId = payload.groupIds[0]
            getGroupHierarchy(rootGroupId)
            getGroupInfo(rootGroupId)
        }
        else if (event.clazz == "group" && event.operation == "hierarchy") {
            groupHierarchy = event.json
        }
        else if (event.clazz == "group" && event.operation == "read") {
            groupInfo = event.items[0].payload as LWEventPayloadGroup
        }

        if (groupHierarchy != null && groupInfo != null) {
            // Loading complete
            server.removeListener(this)
            onComplete(groupHierarchy!!, groupInfo!!)
            RUNNING = false
        }
    }

    override fun onError(error: Throwable) {
        //TODO(sean): implement proper in-app error handling
        Log.e(javaClass.name, "Failed to load group data from server", error)
    }

    fun load(preAuthenticated: Boolean, onComplete: (hierarchy: String, info: LWEventPayloadGroup) -> Unit) {
        if (RUNNING) return
        RUNNING = true

        this.onComplete = onComplete
        if (preAuthenticated) {
            getRootGroupId()
        }
    }

    private fun getRootGroupId() {
        val operation = LWOperation("user", "1", "rootGroups")
        operation.addPayload(LWOperationPayloadGetRootGroups())
        server.command(operation)
    }

    private fun getGroupHierarchy(rootId : String) {
        val operation = LWOperation("group", "1", "hierarchy")
        operation.addPayload(LWOperationPayloadGroup(rootId))
        server.command(operation)
    }

    private fun getGroupInfo(rootId : String) {
        val operation = LWOperation("group", "1", "read")
        operation.addPayload(LWOperationPayloadGroup(rootId))
        server.command(operation)
    }

    companion object {
        @Volatile private var RUNNING = false
    }
}

class LightwaveConfigParser(
        private val groupHierarchy: String,
        private val groupInfo : LWEventPayloadGroup
) {

    private var rooms: List<RoomSpec> = emptyList()
    private var featureSets: Map<String, FeatureSetSpec> = emptyMap()

    private fun JsonReader.skipNameAndValue() {
        skipName()
        skipValue()
    }

    private inline fun JsonReader.readObject(body: () -> Unit) {
        beginObject()
        while (hasNext()) {
            body()
        }
        endObject()
    }

    private inline fun <T : Any> JsonReader.readArray(body: () -> T?) {
        beginArray()
        while (hasNext()) {
            body()
        }
        endArray()
    }

    private inline fun <T : Any> JsonReader.readArrayToList(body: () -> T?): List<T> {
        val result = mutableListOf<T>()
        beginArray()
        while (hasNext()) {
            body()?.let { result.add(it) }
        }
        endArray()
        return result
    }

    fun parse(onRoomFound: (room: RoomEntity, devices: List<DeviceEntity>) -> Unit) {
        val reader = JsonReader.of(Buffer().writeUtf8(groupHierarchy))
        findPayload(reader) {
            reader.readObject {
                when (reader.selectName(JsonReader.Options.of("room", "featureSet"))) {
                    0 -> rooms = parseRooms(reader)
                    1 -> featureSets = parseFeatureSets(reader)
                    else -> reader.skipNameAndValue()
                }
            }
        }

        // Build room and device entities
        rooms.forEach rooms@{ room ->
            val roomEntity = RoomEntity(room.id, room.name)
            val deviceEntities = mutableListOf<DeviceEntity>()
            val existingDevices = mutableSetOf<String>()
            room.featureSets.forEach features@{ featureSetId ->
                if (!featureSets.containsKey(featureSetId)) return@features
                val featureSet = featureSets[featureSetId]!!

                // Device with this name already exists in this room
                if (existingDevices.contains(featureSet.name)) return@features
                existingDevices.add(featureSet.name)

                val device = DeviceEntity(
                        featureSet.id,
                        featureSet.name,
                        findCommand(featureSet.features, "switch"),
                        findCommand(featureSet.features, "dimLevel"),
                        findCommand(featureSet.features, "power"),
                        findCommand(featureSet.features, "energy"),
                        room.id
                )
                device.inferType()
                deviceEntities.add(device)
            }
            onRoomFound(roomEntity, deviceEntities)
        }

    }

    private fun findCommand(features: List<String>, type: String): String? {
        var result: String? = null
        features.forEach { featureId ->
            val featureDetail = groupInfo.features.get(featureId)
            if (featureDetail?.attributes?.type == type) {
                result = featureId
            }
        }
        return result
    }

    private fun findPayload(reader: JsonReader, onFound: () -> Unit) {
        reader.readObject {
            when (reader.selectName(JsonReader.Options.of("items"))) {
                0 -> {
                    reader.readArray {
                        reader.readObject {
                            when (reader.selectName(JsonReader.Options.of("payload"))) {
                                0 -> onFound()
                                else -> reader.skipNameAndValue()
                            }
                        }
                    }
                }
                else -> reader.skipNameAndValue()
            }
        }
    }

    private fun parseRooms(reader: JsonReader) : List<RoomSpec> {
        return reader.readArrayToList { parseRoom(reader) }
    }

    private fun parseRoom(reader: JsonReader) : RoomSpec {
        var roomId = ""
        var roomName = ""
        var featureSetIds : List<String> = emptyList()
        reader.readObject {
            when (reader.selectName(JsonReader.Options.of("groupId", "name", "featureSets"))) {
                0 -> roomId = reader.nextString()
                1 -> roomName = reader.nextString()
                2 -> featureSetIds = reader.readArrayToList { reader.nextString() }
                else -> reader.skipNameAndValue()
            }
        }
        return RoomSpec(roomId, roomName, featureSetIds)
    }

    private fun parseFeatureSets(reader: JsonReader) : Map<String, FeatureSetSpec> {
        val result = mutableMapOf<String, FeatureSetSpec>()
        reader.readArray {
            val entry = parseFeatureSet(reader)
            result[entry.id] = entry
        }
        return result
    }

    private fun parseFeatureSet(reader: JsonReader) : FeatureSetSpec {
        var featureSetId = ""
        var featureSetName = ""
        var featureIds = emptyList<String>()
        reader.readObject {
            when (reader.selectName(JsonReader.Options.of("groupId", "name", "features"))) {
                0 -> featureSetId = reader.nextString()
                1 -> featureSetName = reader.nextString()
                2 -> featureIds = reader.readArrayToList { reader.nextString() }
                else -> reader.skipNameAndValue()
            }
        }
        return FeatureSetSpec(featureSetId, featureSetName, featureIds)
    }
}

data class RoomSpec(
        val id: String,
        val name: String,
        val featureSets: List<String>
)

data class FeatureSetSpec(
        val id: String,
        val name: String,
        val features: List<String>
)