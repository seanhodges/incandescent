package uk.co.seanhodges.incandescent.client.selection

import android.util.Log
import com.squareup.moshi.JsonReader
import okio.Buffer
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.FeaturesetEntity
import uk.co.seanhodges.incandescent.client.storage.FeaturesetHeatingEntity
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import uk.co.seanhodges.incandescent.lightwave.event.*
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGetRootGroups
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

private const val FEATURE_POWER_SWITCH = "switch"
private const val FEATURE_POWER_DIM = "dimLevel"

private const val FEATURE_MONITOR_POWER = "power"
private const val FEATURE_MONITOR_ENERGY = "energy"

private const val FEATURE_HEATING_TEMP_CURRENT = "temperature"
private const val FEATURE_HEATING_TEMP_TARGET = "targetTemperature"
private const val FEATURE_HEATING_VALVE_LEVEL = "valveLevel"
private const val FEATURE_HEATING_STATE = "heatState"
private const val FEATURE_HEATING_BATTERY = "batteryLevel"

private val HIDDEN_DEVICES = arrayOf("energy_monitor")

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

    fun parse(onRoomFound: (room: RoomEntity, devices: List<DeviceEntity>) -> Unit,
              onFeaturesFound: (features: List<FeaturesetEntity>) -> Unit) {

        val reader = JsonReader.of(Buffer().writeUtf8(groupHierarchy))
        var rooms: List<RoomSpec> = emptyList()

        var featureSets: Map<String, FeatureSetSpec> = emptyMap()
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
            val featureEntities = mutableListOf<FeaturesetEntity>()
            val roomEntity = RoomEntity(room.id, room.name)
            val deviceEntities = mutableListOf<DeviceEntity>()
            val existingDevices = mutableSetOf<String>()
            room.featureSets.forEach features@{ featureSetId ->
                if (!featureSets.containsKey(featureSetId)) return@features
                val featureSet = featureSets[featureSetId]!!

                val device = DeviceEntity(
                        featureSet.id,
                        featureSet.name,
                        findCommand(featureSet.features, FEATURE_POWER_SWITCH),
                        findCommand(featureSet.features, FEATURE_POWER_DIM),
                        findCommand(featureSet.features, FEATURE_MONITOR_POWER),
                        findCommand(featureSet.features, FEATURE_MONITOR_ENERGY),
                        room.id
                )
                device.inferType()

                if device is a heating appliance {
                    featureEntities.add(FeaturesetHeatingEntity(
                            deviceId = device.id,
                            currentTempCmd = findCommand(featureSet.features, FEATURE_HEATING_TEMP_CURRENT),
                            targetTempCmd = findCommand(featureSet.features, FEATURE_HEATING_TEMP_TARGET),
                            valveLevelCommand = findCommand(featureSet.features, FEATURE_HEATING_VALVE_LEVEL),
                            heatStateCommand = findCommand(featureSet.features, FEATURE_HEATING_STATE),
                            batteryLevelCommand = findCommand(featureSet.features, FEATURE_HEATING_BATTERY)
                    ))
                }

                // Show only one device that shares the same name in the same room
                // Or if in the list of always-hidden devices
                if (existingDevices.contains(featureSet.name) || HIDDEN_DEVICES.contains(device.type)) {
                    device.hidden = true
                }
                existingDevices.add(featureSet.name)

                deviceEntities.add(device)
            }
            onRoomFound(roomEntity, deviceEntities)
            onFeaturesFound(featureEntities)
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