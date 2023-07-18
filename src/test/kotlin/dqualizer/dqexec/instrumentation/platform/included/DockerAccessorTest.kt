package dqualizer.dqexec.instrumentation.platform.included

import dqualizer.dqlang.types.architecture.RuntimePlatform
import dqualizer.dqlang.types.architecture.ServiceDescription
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.testcontainers.containers.GenericContainer

private const val ContainerName = "dqexec_test_container"
private const val TestImageName = "ghcr.io/dqualizer/dqexec"

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
        serviceDescription.deploymentName = ContainerName

        val accessor = DockerAccessorRuntime()

        accessor.initalize(serviceDescription, RuntimePlatform("1", "Docker", null))
        accessor.connect()
    }

    @Test
    fun canRetrievePID() {

        val serviceDescription = mock(ServiceDescription::class.java)
        serviceDescription.deploymentName = ContainerName

        val accessor = DockerAccessorRuntime()
        accessor.initalize(serviceDescription, RuntimePlatform("1", "Docker", null))

        accessor.getTargetProcessID("java")
    }

}
