package dqualizer.dqexec.output

import dqualizer.dqlang.archive.k6adapter.dqlang.k6.K6Config
import lombok.RequiredArgsConstructor
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Exports the k6 loadtest configuration via RabbitMQ
 */
@Component
@RequiredArgsConstructor
class K6ConfigProducer {
    private val template: RabbitTemplate? = null

    @Value("\${dqualizer.rabbitmq.exchanges.k6:loadtest-k6}")
    private val exchange_name: String = "loadtest-k6"


    private val logger = Logger.getLogger(this.javaClass.name)

    /**
     * Send the k6 configuration to the k6-configuration-runner
     *
     * @param k6Config A loadtest configuration better suited for k6
     * @return String (only for RabbitMQ)
     */
    fun produce(k6Config: K6Config?): String {
        logger.info("Producing k6 configuration\n" + k6Config.toString())

        template!!.convertAndSend(
            exchange_name,
            "POST",
            k6Config!!
        )
        return "K6 LOADTEST CONFIGURATION WAS PRODUCED"
    }
}