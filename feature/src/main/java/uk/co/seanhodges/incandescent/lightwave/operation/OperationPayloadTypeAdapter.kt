package uk.co.seanhodges.incandescent.lightwave.operation

import com.squareup.moshi.*

import java.io.IOException

class OperationPayloadTypeAdapter {

    @ToJson
    @Throws(IOException::class)
    internal fun toJson(writer: JsonWriter, value: LWOperationPayload,
                        payloadFeature: JsonAdapter<LWOperationPayloadFeature>,
                        payloadGetRootGroups: JsonAdapter<LWOperationPayloadGetRootGroups>,
                        payloadConnect: JsonAdapter<LWOperationPayloadConnect>,
                        payloadGroup: JsonAdapter<LWOperationPayloadGroup>) {

        if (value is LWOperationPayloadFeature) {
            payloadFeature.toJson(writer, value)
        } else if (value is LWOperationPayloadGetRootGroups) {
            payloadGetRootGroups.toJson(writer, value)
        } else if (value is LWOperationPayloadConnect) {
            payloadConnect.toJson(writer, value)
        } else if (value is LWOperationPayloadGroup) {
            payloadGroup.toJson(writer, value)
        }
    }

    @FromJson
    @Throws(IOException::class)
    internal fun fromJson(reader: JsonReader): LWOperationPayload? {
        return null
    }

}
