package dqualizer.dqexec.instrumentation.framework.included

import dqualizer.dqexec.instrumentation.framework.InstrumentationPlan
import io.github.dqualizer.dqlang.types.instrumentation.Instrumentation


data class InspectItOcelotInstrumentationPlan(
    val instrumentationDefinition: Instrumentation,
    val inspectItConfiguration: String,
    ) : InstrumentationPlan(
    instrumentationDefinition
)
