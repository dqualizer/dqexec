package dqualizer.dqexec.instrumentation

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenters
import dqualizer.dqexec.instrumentation.framework.included.InspectItOcelotInstrumentationPlanMapper
import dqualizer.dqexec.instrumentation.framework.included.InspectItOcelotInstrumenter
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessors
import dqualizer.dqexec.instrumentation.platform.included.DockerContainerAccessor
import io.github.dqualizer.dqlang.context.DAMRepository
import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.MonitoringConfiguration
import org.springframework.stereotype.Service

@Service
class Monitoring(
    private val domainArchitectureMapping: DomainArchitectureMapping,
    private val platformAccessors: RuntimePlatformAccessors,
    private val runtimeServiceInstrumenters: RuntimeServiceInstrumenters
) {

    fun apply(monitoringConfiguration: MonitoringConfiguration, damID: String) {
      // TODO Currently the DomainArchitectureMapping will be loaded via DAMLoader, will probably change in the future
      //  since the DAM should not be stored in dqexec locally

//        val domainArchitectureMapping =
//            damRepository.findById(damID).orElseThrow { IllegalArgumentException("DAM with ID $damID not found") }


        monitoringConfiguration.serviceMonitoringConfigurations.forEach { monitoringConfig ->
            val targetServiceDescription =
                domainArchitectureMapping.softwareSystem.services.find { service -> service.name == monitoringConfig.serviceID }
                    ?: throw IllegalArgumentException("Service with id ${monitoringConfig.serviceID} not found. Available service ids: ${domainArchitectureMapping.softwareSystem.services.map { it.name }}")

            val serviceRuntimePlatform =  // TODO Why platform.name and not platform.id ?
                domainArchitectureMapping.softwareSystem.runtimePlatforms.find { platform -> platform.name == targetServiceDescription.runtimePlatformId }
                    ?: throw IllegalArgumentException("Platform with id ${targetServiceDescription.runtimePlatformId} not found. Available platform ids: ${domainArchitectureMapping.softwareSystem.runtimePlatforms.map { it.name }}")

            //val platformAccessor = platformAccessors.getPlatformAccessor(serviceRuntimePlatform.name)
            val platformAccessor = DockerContainerAccessor()

            //val serviceInstrumenter = runtimeServiceInstrumenters.getRuntimeServiceInstrumenter(serviceRuntimePlatform.name)
            val serviceInstrumenter = InspectItOcelotInstrumenter(InspectItOcelotInstrumentationPlanMapper())

            serviceInstrumenter.instrument(targetServiceDescription, monitoringConfig, platformAccessor)
        }
    }
}
