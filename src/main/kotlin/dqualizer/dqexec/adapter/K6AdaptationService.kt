package dqualizer.dqexec.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.output.K6ConfigProducer
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
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
    private val producer: K6ConfigProducer,
    private val objectMapper: ObjectMapper
) {
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Import the loadtest configuration and start the adaptation process
     *
     * @param loadTestConfig Imported loadtest configuration
     */
    //TODO: extract and make generic for different load types
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.queues.loadtest}"])
    private fun receive(@Payload loadTestConfig: LoadTestConfig) {
        log.info("Received loadtest configuration\n$loadTestConfig")

//        val parsedConfig = objectMapper.readValue(loadTestConfig, LoadTestConfig::class.java)
        start(loadTestConfig)
    }

    private fun start(loadTestConfig: LoadTestConfig) {
        val k6Config = adapter.adapt(loadTestConfig)
        log.info("### CONFIGURATION ADAPTED ###")
        producer.produce(k6Config)
        log.info("### k6 CONFIGURATION PRODUCED ###")
    }
}
