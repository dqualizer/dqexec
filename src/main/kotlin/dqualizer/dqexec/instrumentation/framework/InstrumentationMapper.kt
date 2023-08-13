package dqualizer.dqexec.instrumentation.framework

import io.github.dqualizer.dqlang.types.instrumentation.Instrumentation

@FunctionalInterface
interface InstrumentationMapper<T : InstrumentationPlan> {
    fun map(instrumentation: Instrumentation): T
}
