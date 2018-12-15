package uk.co.seanhodges.incandescent.client

import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

object Inject {

    private val loadItemIdToFeatureId = mutableMapOf<Int, String>()

    val launch = LaunchActivity()
    val server = LightwaveServer()
    val executor = OperationExecutor(server, loadItemIdToFeatureId)
    val deviceChangeHandler = DeviceChangeHandler(server, loadItemIdToFeatureId)
    val lastValueChangeListener = LastValueChangeListener(server, loadItemIdToFeatureId)

}