package dqualizer.dqexec.input

import dqualizer.dqexec.loadtest.ConfigRunner
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.Config
import lombok.RequiredArgsConstructor
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Imports a k6 loadtest configuration via RabbitMQ
 */
@Component
@RequiredArgsConstructor
class ConfigReceiver {

    @Autowired
    private val runner: ConfigRunner? = null

    /**
     * Import the k6 configuration and start the configuration runner
     * @param config An inofficial k6 configuration
     */
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.queues.k6:k6}"])
    fun receive(@Payload config: Config) {
        runner!!.start(config)
    }
}