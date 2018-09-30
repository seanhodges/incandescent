package uk.co.seanhodges.incandescent.lightwave.event

import com.squareup.moshi.*

import java.io.IOException

class EventPayloadTypeAdapter {

    @ToJson
    @Throws(IOException::class)
    internal fun toJson(writer: JsonWriter, value: LWEventPayload) {
    }

    @FromJson
    @Throws(IOException::class)
    internal fun fromJson(reader: JsonReader,
                          payloadFeature: JsonAdapter<LWEventPayloadFeature>,
                          payloadGetRootGroups: JsonAdapter<LWEventPayloadGetRootGroups>,
                          payloadConnect: JsonAdapter<LWEventPayloadConnect>,
                          payloadGroupResult: JsonAdapter<LWEventPayloadGroup>): LWEventPayload? {

        val value = reader.readJsonValue() as Map<Any, String>

        // Bit hacky here, can't get the operation value now so inferring from
        // the payload fields that are present
        if (value["value"] != null) {
            return payloadFeature.fromJsonValue(value)
        } else if (value["groupIds"] != null) {
            return payloadGetRootGroups.fromJsonValue(value)
        } else if (value["handlerId"] != null) {
            return payloadConnect.fromJsonValue(value)
        } else if (value["features"] != null) {
            return payloadGroupResult.fromJsonValue(value)
        }
        return null
    }

}
