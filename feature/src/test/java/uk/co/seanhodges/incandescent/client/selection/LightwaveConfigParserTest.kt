package uk.co.seanhodges.incandescent.client.selection

import org.hamcrest.CoreMatchers.*
import org.junit.Test

import org.junit.Assert.*
import uk.co.seanhodges.incandescent.client.storage.*
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroupFeature
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroupFeatureAttributes

private const val HIERARCHY_LIGHTS = "/uk/co/seanhodges/incandescent/client/selection/group_hierarchy_lights.json"
private const val HIERARCHY_HEATING = "/uk/co/seanhodges/incandescent/client/selection/group_hierarchy_heating.json"

class LightwaveConfigParserTest {

    @Test
    fun itParsesMultipleRooms() {
        val result = mutableListOf<RoomWithDevices>()
        val groupInfo = LWEventPayloadGroup(mutableMapOf(
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-23-3157332334+1", "dimLevel"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-26-3157332334+1", "switch"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-47-3157332334+1", "dimLevel"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-50-3157332334+1", "switch")
        ))
        val parser = LightwaveConfigParser(aGroupHierarchy(HIERARCHY_LIGHTS), groupInfo)

        parser.parse { room: RoomEntity, devices: List<DeviceEntity> ->
            result.add(RoomWithDevices(room, devices))
        }

        assertThat(result.size, equalTo(2))
        assertThat(result[0].room?.title, equalTo("Living room"))
        assertThat(result[1].room?.title, equalTo("Bedroom"))
    }

    @Test
    fun itParsesADimmer() {
        val result = mutableListOf<RoomWithDevices>()
        val groupInfo = LWEventPayloadGroup(mutableMapOf(
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-23-3157332334+1", "dimLevel"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-26-3157332334+1", "switch")
        ))
        val parser = LightwaveConfigParser(aGroupHierarchy(HIERARCHY_LIGHTS), groupInfo)

        parser.parse { room: RoomEntity, devices: List<DeviceEntity> ->
            result.add(RoomWithDevices(room, devices))
        }

        assertThat(result.size, equalTo(2))
        result[0].also { room ->
            assertThat(room.devices?.size, equalTo(1))
            room.devices!![0].also { device ->
                assertThat(device.title, equalTo("Living room light"))
                assertThat(device.dimCommand, equalTo("5b8aa9b4d36c330fd5b4e100-23-3157332334+1"))
                assertThat(device.powerCommand, equalTo("5b8aa9b4d36c330fd5b4e100-26-3157332334+1"))
            }

        }
    }

    @Test
    fun itParsesAThermostat() {
        val result = mutableListOf<RoomWithDevices>()
        val features = mutableMapOf<String, FeaturesetEntity>()

        val groupInfo = LWEventPayloadGroup(mutableMapOf(
                aGroupFeature("5a9b1d0d34ee367758818b4b-105-3157328757+2", "temperature"),
                aGroupFeature("5a9b1d0d34ee367758818b4b-106-3157328757+2", "targetTemperature")
        ))
        val parser = LightwaveConfigParser(aGroupHierarchy(HIERARCHY_HEATING), groupInfo)

        parser.parse { room: RoomEntity, devices: List<DeviceEntity> ->
            result.add(RoomWithDevices(room, devices))
        }

        assertThat(result.size, equalTo(7))

        result[5].also { room ->
            assertThat(room.devices?.size, equalTo(4))
            room.devices!![1].also { device ->
                assertThat(device.title, equalTo("House Heating"))

                assertThat(features.containsKey(device.id), `is`(true))
                val feature = features[device.id] as FeaturesetHeatingEntity
                assertThat(feature.currentTempCmd, equalTo("5a9b1d0d34ee367758818b4b-105-3157328757+2"))
                assertThat(feature.targetTempCmd, equalTo("5a9b1d0d34ee367758818b4b-106-3157328757+2"))
            }

        }
    }

    private fun aGroupFeature(id: String, type: String): Pair<String, LWEventPayloadGroupFeature> {
        return Pair(id, LWEventPayloadGroupFeature(id, LWEventPayloadGroupFeatureAttributes(type)))
    }

    private fun aGroupHierarchy(file: String): String {
        return this::class.java.getResource(file)!!.readText()
    }
}