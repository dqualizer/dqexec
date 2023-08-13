package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenters
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.instrumentation.Instrumentation
import io.github.dqualizer.dqlang.types.instrumentation.InstrumentationFramework
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class InspectItOcelotInstrumenterTest {

    @Autowired
    lateinit var instrumenterService: RuntimeServiceInstrumenters

    @Test
    fun testExecuteInstrumentation() {
        val instrumentation = Instrumentation(
            listOf(),
            InstrumentationFramework(
                "inspectit-ocelot",
                existing = false,
                enabledMetrics = true,
                enabledTraces = true,
                enabledLogs = true,
                frameworkOptions = emptyMap()
            )
        )
        val accessor = Mockito.mock(RuntimePlatformAccessor::class.java)

        instrumenterService.getInstrumenter("ocelot").instrument(instrumentation, accessor)
    }
}
