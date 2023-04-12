package dqualizer.dqexec.adapter

import dqualizer.dqexec.output.K6ConfigProducer
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Manages the whole adaptation process
 * 1. Adapt the imported loadtest configuration to a k6-configuration
 * 2. Export the k6-configuration
 */
@Component
@Slf4j
public class AdaptationManager(
    private val adapter: K6Adapter,
    private val producer: K6ConfigProducer
) {
    private val log = Logger.getLogger(this.javaClass.name)

    fun start(loadTestConfig: LoadTestConfig) {
        val k6Config = adapter.adapt(loadTestConfig)
        log.info("### CONFIGURATION ADAPTED ###")
        producer.produce(k6Config)
        log.info("### k6 CONFIGURATION PRODUCED ###")
    }
}