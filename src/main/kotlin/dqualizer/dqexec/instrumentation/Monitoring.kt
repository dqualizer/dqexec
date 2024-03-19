package dqualizer.dqexec.instrumentation

import dqualizer.dqexec.instrumentation.framework.RuntimeServiceInstrumenters
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessors
import io.github.dqualizer.dqlang.data.DAMMongoRepository
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.MonitoringConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.stereotype.Service

@Service
@EnableMongoRepositories(basePackageClasses = [DAMMongoRepository::class])
class Monitoring(
    private val damRepository: DAMMongoRepository,
    private val platformAccessors: RuntimePlatformAccessors,
    private val runtimeServiceInstrumenters: RuntimeServiceInstrumenters
) {

    fun apply(monitoringConfiguration: MonitoringConfiguration, damID: String) {
        val domainArchitectureMapping =
            damRepository.findById(damID)
                .orElseThrow { IllegalArgumentException("DAM with ID $damID not found") }


        monitoringConfiguration.serviceMonitoringConfigurations.forEach { monitoringConfig ->

            val targetServiceDescription =
                domainArchitectureMapping.softwareSystem.findArchitectureEntityOfType<ServiceDescription>(
                    monitoringConfig.serviceID
                )
                    .orElseThrow { IllegalArgumentException("Service with id ${monitoringConfig.serviceID} not found. Available service ids: ${domainArchitectureMapping.softwareSystem.services.map { it.id }}") }

            val serviceRuntimePlatform =
                domainArchitectureMapping.softwareSystem.runtimePlatforms
                    .find { platform -> platform.platformId == targetServiceDescription.runtimePlatformId }
                    ?: throw IllegalArgumentException("Platform with id ${targetServiceDescription.id} not found. Available platform ids: ${domainArchitectureMapping.softwareSystem.runtimePlatforms.map { it.name }}")

            val platformAccessor = platformAccessors.getPlatformAccessor(serviceRuntimePlatform.name)

            val serviceInstrumenter =
                runtimeServiceInstrumenters.getRuntimeServiceInstrumenter(targetServiceDescription.instrumentationFramework.name)

            val runTimePlatform =
                domainArchitectureMapping.softwareSystem
                    .findRuntimePlatformById(targetServiceDescription.runtimePlatformId)
                    .orElseThrow { NoSuchElementException("Runtime platform with id ${targetServiceDescription.runtimePlatformId} not found") }

            platformAccessor.setup(targetServiceDescription, runTimePlatform)

            serviceInstrumenter.instrument(
                domainArchitectureMapping,
                targetServiceDescription,
                monitoringConfig,
                platformAccessor
            )
        }
    }
}
