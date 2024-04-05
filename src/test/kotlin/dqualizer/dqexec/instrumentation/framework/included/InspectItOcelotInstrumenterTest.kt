package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenters
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessors
import dqualizer.dqexec.instrumentation.platform.included.DockerContainerAccessor
import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import io.github.dqualizer.dqlang.types.dam.architecture.*
import io.github.dqualizer.dqlang.types.dam.architecture.apischema.APISchema
import io.github.dqualizer.dqlang.types.dam.domainstory.DomainStory
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.instrumentation.InstrumentLocation
import io.github.dqualizer.dqlang.types.rqa.definition.enums.Environment
import org.assertj.core.api.Assertions
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.RabbitMQContainer
import java.lang.module.ModuleDescriptor.Version
import java.lang.reflect.Field
import java.net.URI
import java.util.*

private const val TestImageName = "ghcr.io/dqualizer/dqexec"

var containerName = ""

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

    companion object {

        @JvmStatic
        @BeforeAll
        fun setUp() {
            val network = Network.newNetwork();

            val rabbit = RabbitMQContainer("rabbitmq:management-alpine")
            rabbit.portBindings = listOf("5672:5672", "15672:15672")
            rabbit.start()

            Thread.sleep(4000) //ensure rabbit startup

            val mongoDBContainer = MongoDBContainer("mongo:7")
            mongoDBContainer.portBindings = listOf("27017:27017")
            mongoDBContainer.start()

            val dqexecContainer = GenericContainer<Nothing>(TestImageName)
                .apply {
                    withEnv("spring.rabbitmq.host", rabbit.containerName.substring(1))
                    start()
                }
            containerName = dqexecContainer.containerName.substring(1)
        }
    }

    @Autowired
    lateinit var instrumenterService: RuntimeServiceInstrumenters

    @Autowired
    lateinit var platformAccessorService: RuntimePlatformAccessors

    @Test
    fun testCanInstallOcelotAgent() {
        val random = EasyRandom(EasyRandomParameters()
            .excludeType { type -> type == Version::class.java }
            .randomize({ field: Field -> field.name.equals("location") }) {
                InstrumentLocation("derp.java", "derp.nonexisting.com#IAmAMethodName")
            })

        val instrumenter = instrumenterService.getRuntimeServiceInstrumenter("ocelot")
        val platformAccessors = platformAccessorService.getPlatformAccessor("docker")

        val serviceDescription = ServiceDescription(
            containerName,
            containerName,
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

        val dst = DomainStory(emptyList(), emptyList(), emptyList())
        val system = SoftwareSystem("Dummy", Environment.TEST, emptyList(), emptyList())
        val dam = DomainArchitectureMapping(system, dst)

        instrumenter.instrument(dam, serviceDescription, monitoringConfiguration, platformAccessors)

        Assertions.assertThat(instrumenter).isNotNull
    }
}
