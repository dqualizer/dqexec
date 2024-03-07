package dqualizer.dqexec.instrumentation.framework

import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import org.springframework.plugin.core.Plugin

// TODO: Also add interfaces like LoadTestRunner (set up and run load tests)
//  and ResilienceTestRunner (set up and run resilience tests)
sealed interface IRuntimeServiceInstrumenter : Plugin<String> {

  fun instrument(
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
