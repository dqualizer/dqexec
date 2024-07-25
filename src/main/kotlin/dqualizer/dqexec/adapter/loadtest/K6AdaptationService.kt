package dqualizer.dqexec.adapter.loadtest

import dqualizer.dqexec.loadtest.K6Runner
import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestConfiguration
import org.springframework.stereotype.Service
import java.util.logging.Logger

/**
 * Manages the whole adaptation process
 * 1. Adapt the imported loadtest configuration to a k6-configuration
 * 2. Export the k6-configuration
 */
@Service
class K6AdaptationService(
  private val adapter: K6Adapter,
  private val k6Runner: K6Runner,
) {
  private val log = Logger.getLogger(this.javaClass.name)

  // TODO: extract and make generic for different load types (?)
  /**
   * Adapts the load test configuration to a k6 configuration.
   */
  fun adaptToK6(loadTestConfig: LoadTestConfiguration) {
    log.info("LOAD TEST CONFIGURATION RECEIVED: \n$loadTestConfig")
    val k6Config = adapter.adapt(loadTestConfig)
    log.info("### CONFIGURATION ADAPTED ###")
    log.info("k6 config: $k6Config")
    k6Runner.apply(k6Config)
    log.info("### k6 CONFIGURATION APPLIED ###")
  }
}
