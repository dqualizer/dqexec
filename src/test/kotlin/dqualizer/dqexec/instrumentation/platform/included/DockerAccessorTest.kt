package dqualizer.dqexec.instrumentation.platform.included

import io.github.dqualizer.dqlang.types.architecture.RuntimePlatform
import io.github.dqualizer.dqlang.types.architecture.ServiceDescription
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.testcontainers.containers.GenericContainer

private const val ContainerName = "dqexec_test_container"
private const val TestImageName = "ghcr.io/dqualizer/dqexec"

private const val AccessorType = "Docker"

class DockerAccessorTest {
    companion object {
        val grafanaContainer = GenericContainer<Nothing>(TestImageName)
            .apply {
                withCreateContainerCmdModifier { cmd -> cmd.withName(ContainerName) }
                start()
            }
    }

    @Test
    fun canConnectToContainer() {

        val serviceDescription = mock(ServiceDescription::class.java)
        Mockito.`when`(serviceDescription.getDeploymentName()).thenReturn(ContainerName)

        val accessor = DockerContainerAccessor()

        accessor.setup(serviceDescription, RuntimePlatform("1", AccessorType, null))
        accessor.connect()
    }

    @Test
    fun canDisconnectFromContainer() {
        val serviceDescription = mock(ServiceDescription::class.java)
        Mockito.`when`(serviceDescription.getDeploymentName()).thenReturn(ContainerName)

        val accessor = DockerContainerAccessor()
        accessor.setup(serviceDescription, RuntimePlatform("1", AccessorType, null))
        accessor.connect()
        accessor.disconnect()
    }


    @Test
    fun canRetrievePID() {
        val serviceDescription = mock(ServiceDescription::class.java)
        Mockito.`when`(serviceDescription.getDeploymentName()).thenReturn(ContainerName)

        val accessor = DockerContainerAccessor()
        accessor.setup(serviceDescription, RuntimePlatform("1", AccessorType, null))

        val targetProcessID = accessor.getTargetProcessID("java")
        Assertions.assertNotNull(targetProcessID)
        Assertions.assertTrue(targetProcessID > 0)
    }
}
