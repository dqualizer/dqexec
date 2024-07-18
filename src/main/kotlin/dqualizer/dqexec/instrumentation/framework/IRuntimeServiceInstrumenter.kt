package dqualizer.dqexec.instrumentation.framework

import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import org.springframework.plugin.core.Plugin
import org.springframework.stereotype.Service

@Service
interface IRuntimeServiceInstrumenter : Plugin<String> {

  fun instrument(
    dam: DomainArchitectureMapping,
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  )

  fun deinstrument(
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  )
}
