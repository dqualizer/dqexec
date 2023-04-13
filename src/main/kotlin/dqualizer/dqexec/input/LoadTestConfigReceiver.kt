package dqualizer.dqexec.input

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.adapter.AdaptationManager
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.logging.Logger

/**
 * Imports a dqlang loadtest configuration via RabbitMQ
 */
@Service
class LoadTestConfigReceiver(private val manager: AdaptationManager, private val objectMapper: ObjectMapper) {

    private val logger = Logger.getLogger(this.javaClass.name)

    /**
     * Import the loadtest configuration and start the adaptation process
     *
     * @param loadTestConfig Imported loadtest configuration
     */
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.queues.loadtest}"])
    fun receive(@Payload loadTestConfig: String) {
        logger.info("Received loadtest configuration\n" + loadTestConfig)
        val loadTestConfig = objectMapper.readValue(loadTestConfig, LoadTestConfig::class.java)
        manager.start(loadTestConfig)
    }
}
