package dqualizer.dqexec.instrumentation.framework

import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import org.springframework.stereotype.Service

@Service
abstract class RuntimeServiceInstrumenter : IRuntimeServiceInstrumenter {

  final override fun instrument(
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  ) {
    executeInstrumentationPlan(targetService, serviceMonitoringConfiguration, platformAccessor)
  }

  final override fun deinstrument(
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  ) {
    revertInstrumentationPlan(targetService, serviceMonitoringConfiguration, platformAccessor)
  }

  // TODO Do we need those two extra methods? Can't we use the already provided ones?
  protected abstract fun executeInstrumentationPlan(
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  )

  protected abstract fun revertInstrumentationPlan(
    targetService: ServiceDescription,
    serviceMonitoringConfiguration: ServiceMonitoringConfiguration,
    platformAccessor: RuntimePlatformAccessor
  )
}
