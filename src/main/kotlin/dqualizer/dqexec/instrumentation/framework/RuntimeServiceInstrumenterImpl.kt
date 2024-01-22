package dqualizer.dqexec.instrumentation.framework

import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.instrumentation.Instrumentation
import org.springframework.stereotype.Service

@Service
abstract class RuntimeServiceInstrumenterImpl<I : InstrumentationPlan> : RuntimeServiceInstrumenter<I> {

    protected abstract val instrumentationMapper: InstrumentationMapper<I>

    final override fun instrument(instrumentation: Instrumentation, platformAccessor: RuntimePlatformAccessor) {
        executeInstrumentationPlan(instrumentationMapper.map(instrumentation), platformAccessor)
    }

    final override fun deinstrument(instrumentation: Instrumentation, platformAccessor: RuntimePlatformAccessor) {
        reverseInstrumentationPlan(instrumentationMapper.map(instrumentation), platformAccessor)
    }

    protected abstract fun executeInstrumentationPlan(
        instrumentation: I,
        platformAccessor: RuntimePlatformAccessor
    )

    protected abstract fun reverseInstrumentationPlan(
        instrumentation: I,
        platformAccessor: RuntimePlatformAccessor
    )
}
