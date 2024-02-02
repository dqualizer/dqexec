package dqualizer.dqexec.instrumentation

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenters
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessors
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
//        val domainArchitectureMapping =
//            damRepository.findById(damID).orElseThrow { IllegalArgumentException("DAM with ID $damID not found") }


        monitoringConfiguration.serviceMonitoringConfigurations.forEach { monitoringConfig ->
            val targetServiceDescription =
                domainArchitectureMapping.softwareSystem.services.find { service -> service.id == monitoringConfig.serviceID }
                    ?: throw IllegalArgumentException("Service with id ${monitoringConfig.serviceID} not found. Available service ids: ${domainArchitectureMapping.softwareSystem.services.map { it.name }}")

            val serviceRuntimePlatform =
                domainArchitectureMapping.softwareSystem.runtimePlatforms.find { platform -> platform.name == targetServiceDescription.runtimePlatformId }
                    ?: throw IllegalArgumentException("Platform with id ${targetServiceDescription.runtimePlatformId} not found. Available platform ids: ${domainArchitectureMapping.softwareSystem.runtimePlatforms.map { it.name }}")

            val platformAccessor = platformAccessors.getPlatformAccessor(serviceRuntimePlatform.name)

            val serviceInstrumenter =
                runtimeServiceInstrumenters.getRuntimeServiceInstrumenter(serviceRuntimePlatform.name)

            serviceInstrumenter.instrument(targetServiceDescription, monitoringConfig, platformAccessor)
        }
    }
}
