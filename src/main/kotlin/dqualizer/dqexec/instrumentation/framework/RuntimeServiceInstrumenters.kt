package dqualizer.dqexec.instrumentation.framework

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service


@Service
class RuntimeServiceInstrumenters {

  private val log = KotlinLogging.logger {}

  lateinit var registry: PluginRegistry<IRuntimeServiceInstrumenter, String>

  @Autowired
  fun setInstrumenters(instrumenters: List<IRuntimeServiceInstrumenter>) {
    registry = PluginRegistry.of(instrumenters)

    val availableRuntimeInstrumenters = instrumenters.map { it.javaClass.canonicalName }
    log.info { "Registered ${instrumenters.size} RuntimeServiceInstrumenters: $availableRuntimeInstrumenters" }
  }

  fun getRuntimeServiceInstrumenter(platformTypeIdentifier: String): IRuntimeServiceInstrumenter {
    return registry.getPluginFor(platformTypeIdentifier)
      .orElseThrow { IllegalArgumentException("No instrumenter found for delimiter $platformTypeIdentifier") }
  }
}
