package dqualizer.dqexec.instrumentation.framework.included

import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration


data class InspectItOcelotInstrumentationPlan(
    val instrumentationDefinition: ServiceMonitoringConfiguration,
    val inspectItConfiguration: String
)
