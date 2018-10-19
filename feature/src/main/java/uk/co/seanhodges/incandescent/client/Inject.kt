package uk.co.seanhodges.incandescent.client

import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

object Inject {

    val loadItemIdToFeatureId = mutableMapOf<Int, String>()
    val server = LightwaveServer()
    val executor = OperationExecutor(server, loadItemIdToFeatureId)
    val deviceChangeHandler = DeviceChangeHandler(server, loadItemIdToFeatureId)

}