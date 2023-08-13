package dqualizer.dqexec.instrumentation.framework

import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.instrumentation.Instrumentation
import org.springframework.plugin.core.Plugin

sealed interface RuntimeServiceInstrumenter<I : InstrumentationPlan> : Plugin<String> {

    fun instrument(instrumentation: Instrumentation, platformAccessor: RuntimePlatformAccessor)

    fun deinstrument(instrumentation: Instrumentation, platformAccessor: RuntimePlatformAccessor)
}
