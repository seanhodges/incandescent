package uk.co.seanhodges.incandescent.lightwave.event

import com.squareup.moshi.Json

data class LWEvent(
        val version: Int,
        val senderId: String,
        val transactionId: Int,
        val operation: String,
        val direction: String,
        val items: MutableList<LWEventItem>,
        @field:Json(name = "class") val clazz: String
) {
    @Transient var json: String = ""
}

data class LWEventItem (
        val itemId: Int,
        val payload: LWEventPayload
)

interface LWEventPayload

data class LWEventPayloadConnect (
        val handlerId: String
) : LWEventPayload

data class LWEventPayloadFeature (
        val value: Int,
        val status: String
) : LWEventPayload {
    var featureId: String? = ""
}

data class LWEventPayloadGetRootGroups (
        val groupIds: List<String>
) : LWEventPayload

data class LWEventPayloadGroup (
        val features: Map<String, LWEventPayloadGroupFeature>
) : LWEventPayload

data class LWEventPayloadGroupFeature (
        val featureId: String,
        val attributes: LWEventPayloadGroupFeatureAttributes
)

data class LWEventPayloadGroupFeatureAttributes (
        val type: String
)