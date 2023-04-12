package dqualizer.dqexec.adapter

import org.springframework.beans.factory.annotation.Autowired
import dqualizer.dqexec.adapter.K6Adapter
import dqualizer.dqexec.output.K6ConfigProducer
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.K6Config
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Manages the whole adaptation process
 * 1. Adapt the imported loadtest configuration to a k6-configuration
 * 2. Export the k6-configuration
 */
@Component
class AdaptationManager {
    private val logger = Logger.getLogger(this.javaClass.name)

    @Autowired
    private val adapter: K6Adapter? = null

    @Autowired
    private val producer: K6ConfigProducer? = null
    fun start(loadTestConfig: LoadTestConfig?) {
        val k6Config = adapter!!.adapt(loadTestConfig)
        logger.info("### CONFIGURATION ADAPTED ###")
        producer!!.produce(k6Config)
        logger.info("### k6 CONFIGURATION PRODUCED ###")
    }
}