package uk.co.seanhodges.incandescent.client

import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

object Inject {

    val server = LightwaveServer()
    val executor = OperationExecutor(server)
    val deviceChangeHandler = DeviceChangeHandler(server)

}