package dqualizer.dqexec.instrumentation.framework.included

import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import io.github.dqualizer.dqlang.types.dam.architecture.SoftwareSystem
import io.github.dqualizer.dqlang.types.dam.domainstory.DomainStory
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.instrumentation.InstrumentLocation
import io.github.dqualizer.dqlang.types.rqa.definition.enums.Environment
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class InspectItOcelotInstrumentationPlanMapperTest {


    @Test
    fun tryLoading() {
        val easyRandom =
            EasyRandom(EasyRandomParameters().randomize({ field: Field -> field.name.equals("location") }) {
                   InstrumentLocation("derp.java",  "derp.nonexisting.com#IAmAMethodName")
            })
        val serviceMonitoringConfiguration = easyRandom.nextObject(ServiceMonitoringConfiguration::class.java)
//
//        val objectMapper = ObjectMapper(YAMLFactory())
//        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        val mapper = InspectItOcelotInstrumentationPlanMapper()
        val dst = DomainStory(emptyList(), emptyList(), emptyList())
        val system = SoftwareSystem("Dummy", Environment.TEST, emptyList(), emptyList())
        val dam = DomainArchitectureMapping(system, dst)

        val instrumentationPlan = mapper.map(serviceMonitoringConfiguration, dam)
        println(instrumentationPlan.inspectItConfiguration)

    }
}
