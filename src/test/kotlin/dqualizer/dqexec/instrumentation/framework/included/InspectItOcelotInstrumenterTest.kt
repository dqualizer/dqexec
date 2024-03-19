package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenters
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessors
import dqualizer.dqexec.instrumentation.platform.included.DockerContainerAccessor
import io.github.dqualizer.dqlang.types.dam.architecture.InstrumentationFramework
import io.github.dqualizer.dqlang.types.dam.architecture.ProgrammingFramework
import io.github.dqualizer.dqlang.types.dam.architecture.RuntimePlatform
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.dam.architecture.apischema.APISchema
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import org.assertj.core.api.Assertions
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.lang.module.ModuleDescriptor.Version
import java.net.URI
import java.util.*

@SpringBootTest(
    classes = [
        RuntimeServiceInstrumenters::class,
        RuntimePlatformAccessors::class,
        DockerContainerAccessor::class,
        InspectItOcelotInstrumenter::class,
        InspectItOcelotInstrumentationPlanMapper::class]
)
@ActiveProfiles("test")
class InspectItOcelotInstrumenterTest {

    @Autowired
    lateinit var instrumenterService: RuntimeServiceInstrumenters

    @Autowired
    lateinit var platformAccessorService: RuntimePlatformAccessors

    @Test
    fun testCanInstallocelotAgent() {
        val random = EasyRandom(EasyRandomParameters().excludeType { type -> type == Version::class.java })

        val instrumenter = instrumenterService.getRuntimeServiceInstrumenter("ocelot")
        val platformAccessors = platformAccessorService.getPlatformAccessor("docker")

        val serviceDescription = ServiceDescription(
            "assignment-service",
            "assignment-service",
            URI("http://localhost:8080"),
            random.nextObject(ProgrammingFramework::class.java),
            "java",
            InstrumentationFramework("inspectit", false),
            "1",
            Collections.emptySet(),
            random.nextObject(APISchema::class.java),
            Collections.emptySet()
        )

        val monitoringConfiguration = random.nextObject(ServiceMonitoringConfiguration::class.java)


        val runtimePlatform = RuntimePlatform("1", "docker", null)

        platformAccessors.setup(serviceDescription, runtimePlatform)

        instrumenter.instrument("IAmTheContextId", serviceDescription, monitoringConfiguration, platformAccessors)

        Assertions.assertThat(instrumenter).isNotNull
    }
}
