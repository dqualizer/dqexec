package dqualizer.dqexec.input

import dqualizer.dqexec.loadtest.ConfigRunner
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.Config
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.logging.Logger

/**
 * Imports a k6 loadtest configuration via RabbitMQ
 */
@Service
class ConfigReceiver(private val runner: ConfigRunner) {

    private val logger = Logger.getLogger(this.javaClass.name)

    /**
     * Import the k6 configuration and start the configuration runner
     * @param config An inofficial k6 configuration
     */
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.queues.k6}"])
    fun receive(@Payload config: Config) {
        logger.info("Received k6 configuration\n" + config.toString())
        runner!!.start(config)
    }
}