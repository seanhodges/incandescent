package uk.co.seanhodges.incandescent.client.selection

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test

import org.junit.Assert.*
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadFeature
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroupFeature
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroupFeatureAttributes

class LightwaveConfigParserTest {

    @Test
    fun itParsesTheHierarchyAndEmitsSeveralRooms() {
        val result = mutableListOf<RoomWithDevices>()
        val groupHierarchy = javaClass.getResource("/uk/co/seanhodges/incandescent/client/selection/group_hierarchy.json")!!.readText()
        val groupInfo = LWEventPayloadGroup(mutableMapOf(
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-23-3157332334+1", "dimLevel"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-26-3157332334+1", "switch"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-47-3157332334+1", "dimLevel"),
                aGroupFeature("5b8aa9b4d36c330fd5b4e100-50-3157332334+1", "switch")
        ))
        val parser = LightwaveConfigParser(groupHierarchy, groupInfo)

        parser.parse { room: RoomEntity, devices: List<DeviceEntity> ->
            result.add(RoomWithDevices(room, devices))
        }

        assertThat(result.size, equalTo(2))
        assertThat(result[0].room?.title, equalTo("Living room"))
        assertThat(result[0].devices?.size, equalTo(1))
        assertThat(result[0].devices!![0].title, equalTo("Living room light"))
        assertThat(result[0].devices!![0].dimCommand, equalTo("5b8aa9b4d36c330fd5b4e100-23-3157332334+1"))
        assertThat(result[0].devices!![0].powerCommand, equalTo("5b8aa9b4d36c330fd5b4e100-26-3157332334+1"))
        assertThat(result[0].devices?.size, equalTo(1))
        assertThat(result[1].room?.title, equalTo("Bedroom"))
        assertThat(result[1].devices?.size, equalTo(1))
        assertThat(result[1].devices!![0].title, equalTo("Bedroom light"))
        assertThat(result[1].devices!![0].dimCommand, equalTo("5b8aa9b4d36c330fd5b4e100-47-3157332334+1"))
        assertThat(result[1].devices!![0].powerCommand, equalTo("5b8aa9b4d36c330fd5b4e100-50-3157332334+1"))

    }

    private fun aGroupFeature(id: String, type: String): Pair<String, LWEventPayloadGroupFeature> {
        return Pair(id, LWEventPayloadGroupFeature(id, LWEventPayloadGroupFeatureAttributes(type)))
    }
}