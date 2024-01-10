package dqualizer.dqexec.instrumentation.platform

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service

@Service
class RuntimePlatformAccessors {

    private val log = KotlinLogging.logger {}

    lateinit var registry: PluginRegistry<RuntimePlatformAccessor, String>

    @Autowired
    fun setAccessors(accessors: List<RuntimePlatformAccessor>) {
        registry = PluginRegistry.of(accessors)
        log.info  { "Activated ${accessors.size} RuntimePlatformAccessors" }
        log.debug { "Activated RuntimePlatformAccessors: ${accessors.map { it.javaClass.canonicalName }}" }
    }

    fun getPlatformAccessor(platformName: String): RuntimePlatformAccessor {
        return registry.getPluginFor(platformName).orElseThrow()
    }
}
