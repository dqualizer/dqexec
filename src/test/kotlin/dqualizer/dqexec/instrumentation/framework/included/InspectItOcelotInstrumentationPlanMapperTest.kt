package dqualizer.dqexec.instrumentation.framework.included

import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.Test

class InspectItOcelotInstrumentationPlanMapperTest {

    @Test
    fun tryLoading() {
        val easyRandom = EasyRandom()
        val serviceMonitoringConfiguration = easyRandom.nextObject(ServiceMonitoringConfiguration::class.java)
//
//        val objectMapper = ObjectMapper(YAMLFactory())
//        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        val mapper = InspectItOcelotInstrumentationPlanMapper()
        val instrumentationPlan = mapper.map(serviceMonitoringConfiguration, "dummy")

        println(instrumentationPlan.inspectItConfiguration)

    }
}
