package dqualizer.dqexec.input

import dqualizer.dqexec.adapter.AdaptationManager
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.logging.Logger
import kotlin.math.log

/**
 * Imports a dqlang loadtest configuration via RabbitMQ
 */
@Service
class LoadTestConfigReceiver(private val manager: AdaptationManager) {

    private val logger = Logger.getLogger(this.javaClass.name)

    /**
     * Import the loadtest configuration and start the adaptation process
     *
     * @param loadTestConfig Imported loadtest configuration
     */
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.exchanges.loadtest:translator-loadtest}"])
    fun receive(@Payload loadTestConfig: LoadTestConfig?) {
        logger.info("Received loadtest configuration\n" + loadTestConfig.toString())
        manager.start(loadTestConfig!!)
    }
}
