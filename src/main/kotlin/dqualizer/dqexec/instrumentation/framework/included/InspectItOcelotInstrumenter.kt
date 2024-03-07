package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenter
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private const val INSPECT_IT_OCELOT_VERSION = "2.6.4"

private const val INSPECTIT_OCELOT_JAR = "inspectit-ocelot-agent-$INSPECT_IT_OCELOT_VERSION.jar"

/**
 * @author Lion Wagner
 */
@Component
class InspectItOcelotInstrumenter(
    val instrumentationMapper: InspectItOcelotInstrumentationPlanMapper
) : RuntimeServiceInstrumenter() {

    private val log = KotlinLogging.logger { }

    override fun executeInstrumentationPlan(
        targetService: ServiceDescription,
        serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
        platformAccessor: RuntimePlatformAccessor
    ) {
        val instrumentationPlan = instrumentationMapper.map(serviceMonitoringConfiguration, targetService.name)

        platformAccessor.connect()

        log.info { "Connected to platform" }


        // TODO:
        //  - check if container has internet access, otherwise try download locally
        //  - location of the jar should be configurable

        log.info { "Downloading agent" }
        var response = platformAccessor.executeInServiceContainer(
            """
                curl https://github.com/inspectIT/inspectit-oce/releases/download/$INSPECT_IT_OCELOT_VERSION/$INSPECTIT_OCELOT_JAR -o /tmp/$INSPECTIT_OCELOT_JAR
            """.trimIndent()
        )
        if (response.contains("curl: not found")) {
            log.debug { "curl not found, trying wget" }
            response = platformAccessor.executeInServiceContainer(
                """
                    wget https://github.com/inspectIT/inspectit-oce/releases/download/$INSPECT_IT_OCELOT_VERSION/$INSPECTIT_OCELOT_JAR -O /tmp/$INSPECTIT_OCELOT_JAR
                """.trimIndent()
            )
        }

        log.debug { "Response: $response" }

        val targetProcessId = platformAccessor.getTargetProcessID("java")
        log.info { "Target process id: $targetProcessId" }


        log.info { "Starting agent" }
        val config = instrumentationPlan.inspectItConfiguration
        val cmd = """
            java -jar /tmp/$INSPECTIT_OCELOT_JAR $targetProcessId '$config'
        """.trimIndent()
        val agentResponse = platformAccessor.executeInServiceContainer(cmd)

        log.debug { "Response: $agentResponse" }

        if(!agentResponse.contains("Agent successfully attached!")) {
            throw RuntimeException("Agent could not be started")
        }
    }

    override fun revertInstrumentationPlan(
        targetService: ServiceDescription,
        serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
        platformAccessor: RuntimePlatformAccessor
    ) {
        TODO("Not yet implemented")
    }

    override fun supports(delimiter: String): Boolean {
        return listOf("inspectit", "ocelot", "inspectit_ocelot").contains(delimiter.lowercase())
    }
}
