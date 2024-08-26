package dqualizer.dqexec.adapter.resilience

import dqualizer.dqexec.resilience.CtkRunner
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class CtkAdaptationService(
  private val adapter: CtkAdapter,
  private val ctkRunner: CtkRunner,
) {
    private val log = Logger.getLogger(this.javaClass.name)

  /**
   * Adapts the configuration to chaos toolkit
   */
   fun adaptToCtk(@Payload resilienceTestConfig: ResilienceTestConfiguration) {
     log.info("RESILIENCE TEST CONFIGURATION RECEIVED: \n$resilienceTestConfig")
     val ctkConfiguration = adapter.adapt(resilienceTestConfig)
     log.info("### CONFIGURATION ADAPTED ###")
     log.info("CHAOS-TOOLKIT CONFIG: \n$ctkConfiguration")
     ctkRunner.start(ctkConfiguration)
    }
}
