package uk.co.seanhodges.incandescent.lightwave.event

interface LWEventListener {

    fun onEvent(event: LWEvent)
    fun onError(error: Throwable)
    fun onRawEvent(packet: String) { }
}
