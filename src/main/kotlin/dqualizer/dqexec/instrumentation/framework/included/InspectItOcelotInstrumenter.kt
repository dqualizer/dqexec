package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.InstrumentationMapper
import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenterImpl
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import org.springframework.stereotype.Component

/**
 * @author Lion Wagner
 */
@Component
class InspectItOcelotInstrumenter(
    override val instrumentationMapper: InstrumentationMapper<InspectItOcelotInstrumentationPlan>
) : RuntimeServiceInstrumenterImpl<InspectItOcelotInstrumentationPlan>() {

    override fun executeInstrumentationPlan(
        instrumentationPlan: InspectItOcelotInstrumentationPlan,
        platformAccessor: RuntimePlatformAccessor
    ) {
        platformAccessor.connect()

        val targetProccessId = platformAccessor.getTargetProcessID("java")

        //TODOs:
        // - check if container has internet access, otherwise try download locally
        // - location of the jar should be configurable

        platformAccessor.executeInServiceContainer(
            """
                wget https://github.com/inspectIT/inspectit-oce/releases/download/2.5.3/inspectit-ocelot-agent-2.5.3.jar
                java -jar inspectit-ocelot-agent-2.5.3.jar $targetProccessId '${instrumentationPlan.inspectItConfiguration}'                
            """.trimIndent()
        )
    }

    override fun reverseInstrumentationPlan(
        instrumentation: InspectItOcelotInstrumentationPlan,
        platformAccessor: RuntimePlatformAccessor
    ) {
        TODO("Not yet implemented")
    }

    override fun supports(delimiter: String): Boolean {
        return listOf("inspectit", "ocelot", "inspectit_ocelot").contains(delimiter.lowercase())
    }
}
