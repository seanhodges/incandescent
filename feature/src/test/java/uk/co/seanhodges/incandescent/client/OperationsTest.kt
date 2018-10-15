package uk.co.seanhodges.incandescent.client

import de.jodamob.kotlin.testrunner.KotlinTestRunner
import de.jodamob.kotlin.testrunner.OpenedPackages
import org.hamcrest.core.Is.`is`
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperation
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import org.mockito.ArgumentCaptor
import org.mockito.junit.MockitoJUnitRunner
import uk.co.seanhodges.incandescent.lightwave.operation.LWOperationPayloadFeature

@RunWith(MockitoJUnitRunner::class)
class OperationsTest {

    private var server = Mockito.mock(LightwaveServer::class.java)
    val operationExecutor = OperationExecutor(server)

    @Test
    fun itProcessesAChangeOperation() {
        operationExecutor.enqueueChange("FEATURE1", 50)
        operationExecutor.processOperations()

        verify(server, times(1)).command(any(LWOperation::class.java))
    }

    @Test
    fun itProcessesMultipleChanges() {
        operationExecutor.enqueueChange("FEATURE1", 50)
        operationExecutor.enqueueChange("FEATURE2", 0)
        operationExecutor.processOperations()

        verify(server, times(2)).command(any(LWOperation::class.java))
    }

    @Test
    fun itPreventsMultipleValueChangesOfTheSameFeature() {
        operationExecutor.enqueueChange("FEATURE1", 50)
        operationExecutor.enqueueChange("FEATURE1", 0)
        operationExecutor.processOperations()

        val argument = ArgumentCaptor.forClass(LWOperation::class.java)
        verify(server, times(1)).command(argument.capture())
        assertThat((argument.value.items[0].payload as LWOperationPayloadFeature).value, `is`(0))
    }
}