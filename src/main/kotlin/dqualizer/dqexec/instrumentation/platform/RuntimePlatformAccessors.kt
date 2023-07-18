package dqualizer.dqexec.instrumentation.platform

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service

@Service
class RuntimePlatformAccessors() {

    lateinit var registry: PluginRegistry<RuntimePlatformAccessor, String>

    @Autowired
    fun setAccessors(accessors: List<RuntimePlatformAccessor>) {
        registry = PluginRegistry.of(accessors)
    }

    fun getPlatformName(platformName: String): RuntimePlatformAccessor {
        return registry.getPluginFor(platformName).orElseThrow()
    }

}
