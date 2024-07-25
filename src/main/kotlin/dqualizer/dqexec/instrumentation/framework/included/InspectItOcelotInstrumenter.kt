package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.IRuntimeServiceInstrumenter
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

private const val INSPECT_IT_OCELOT_VERSION = "2.6.1"

private const val INSPECTIT_OCELOT_JAR = "inspectit-ocelot-agent-$INSPECT_IT_OCELOT_VERSION.jar"

/**
 * @author Lion Wagner
 */
@Component
class InspectItOcelotInstrumenter(
  val instrumentationMapper: InspectItOcelotInstrumentationPlanMapper
) : IRuntimeServiceInstrumenter {

  val supportedNames = listOf("inspectit", "ocelot", "inspectit_ocelot", "InspectIT Ocelot")
    .map { it.lowercase() }.toSet() //case insensitivity

  private val log = KotlinLogging.logger { }


  override fun instrument(
    dam: DomainArchitectureMapping,
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  ) {
    val instrumentationPlan = instrumentationMapper.map(serviceMonitoringConfiguration, dam)


    val output = Files.writeString(
      Path.of("configuration/plan.yaml"),
      instrumentationPlan.inspectItConfiguration,
      Charsets.UTF_8
    )

    log.info { "InspectIT Ocelot configuration written to ${output.toAbsolutePath()}" }

    //platformAccessor.connect()

    //log.info { "Connected to platform" }



    //TODOs:
    // - check if container has internet access, otherwise try download locally
    // - location of the jar should be configurable

/*    log.info { "Downloading agent" }
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
    }*/

   /* log.debug { "Response: $response" }

    val targetProcessId = platformAccessor.getTargetProcessID("java")
    log.info { "Target process id: $targetProcessId" }


    log.info { "Starting agent" }
    val config = instrumentationPlan.inspectItConfiguration

    //write config to file (in container)
    platformAccessor.executeInServiceContainer("echo $config > /tmp/inspectit-config.json")

    val cmd = """
            export INSPECTIT_CONFIG_FILE_BASED_ENABLED=true && \
            export INSPECTIT_CONFIG_FILE_BASED_PATH=/tmp/inspectit-config.json && \
            java -jar /tmp/$INSPECTIT_OCELOT_JAR $targetProcessId            
        """.trimIndent()
    val agentResponse = platformAccessor.executeInServiceContainer(cmd)

    log.debug { "Response: $agentResponse" }

    if (!agentResponse.contains("Agent successfully attached!")) {
      throw RuntimeException("Agent could not be started")
    }*/
  }

  override fun deinstrument(
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  ) {
    TODO("Not yet implemented")
  }

  override fun supports(delimiter: String): Boolean {

    return supportedNames.contains(delimiter.lowercase())
  }
}
