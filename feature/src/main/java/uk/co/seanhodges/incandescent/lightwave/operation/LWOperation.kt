package uk.co.seanhodges.incandescent.lightwave.operation

import com.squareup.moshi.Json

data class LWOperation(
        @field:Json(name = "class") val clazz: String,
        val senderId: String,
        val operation : String
) {
    val version : Int = 1
    val direction : String = "request"
    val items : MutableList<LWOperationItem> = ArrayList()

    var transactionId: Int = 1

    fun addPayload(payload: LWOperationPayload) {
        items.add(LWOperationItem(items.size + 1, payload))
    }
}

data class LWOperationItem (
        val itemId : Int = 0,
        val payload : LWOperationPayload
)

interface LWOperationPayload

data class LWOperationPayloadConnect (
        val token : String,
        val clientDeviceId: String
) : LWOperationPayload

data class LWOperationPayloadFeature (
        val featureId: String,
        val value: Int = 0
) : LWOperationPayload

class LWOperationPayloadGetRootGroups : LWOperationPayload

data class LWOperationPayloadGroup (
        val groupId : String,
        val blocks : Boolean = false,
        val devices : Boolean = false,
        val features : Boolean = true,
        val scripts : Boolean = false,
        val subgroups : Boolean = true,
        val subgroupDepth : Int = 10
) : LWOperationPayload