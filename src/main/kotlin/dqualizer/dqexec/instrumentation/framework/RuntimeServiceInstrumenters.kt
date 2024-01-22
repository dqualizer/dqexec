package dqualizer.dqexec.instrumentation.framework

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service

@Service
class RuntimeServiceInstrumenters {
    lateinit var registry: PluginRegistry<RuntimeServiceInstrumenter<*>, String>


    @Autowired
    fun setInstrumenters(instrumenters: List<RuntimeServiceInstrumenter<*>>) {
        registry = PluginRegistry.of(instrumenters)
    }


    fun getInstrumenter(delimiter: String): RuntimeServiceInstrumenter<*> {
        return registry.getPluginFor(delimiter)
            .orElseThrow { IllegalArgumentException("No instrumenter found for delimiter $delimiter") }
    }
}
