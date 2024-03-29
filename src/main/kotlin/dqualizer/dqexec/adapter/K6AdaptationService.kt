package dqualizer.dqexec.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.output.K6ConfigProducer
// import dqualizer.dqexec.output.K6ConfigProducer
import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestConfiguration
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
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
   private val producer: K6ConfigProducer
) {
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Import the loadtest configuration and start the adaptation process
     *
     * @param loadTestConfig Imported loadtest configuration
     */
    //TODO: extract and make generic for different load types
    @RabbitListener(queues = ["\${dqualizer.messaging.queues.rqaConfigurationProducerQueue.name}"])
    private fun receive(@Payload loadTestConfig: LoadTestConfiguration) {
        log.info("Received loadtest configuration\n$loadTestConfig")
        start(loadTestConfig)
    }

    private fun start(loadTestConfig: LoadTestConfiguration  ) {
        val k6Config = adapter.adapt(loadTestConfig)
        log.info("### CONFIGURATION ADAPTED ###")
        log.info("k6 config" + k6Config)
        producer.produce(k6Config)
         log.info("### k6 CONFIGURATION PRODUCED ###")
    }
}
