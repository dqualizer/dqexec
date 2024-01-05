package dqualizer.dqexec.output

import io.github.dqualizer.dqlang.types.adapter.k6.K6Configuration
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Exports the k6 loadtest configuration via RabbitMQ
 */
@Component
class K6ConfigProducer(
        val template: RabbitTemplate,
        val messageConverter: MessageConverter
) {

    @Value("\${dqualizer.messaging.queues.k6.name}")
    private lateinit var k6QueueRoutingKey: String


    private val logger = Logger.getLogger(this.javaClass.name)

    /**
     * Send the k6 configuration to the k6-configuration-runner
     *
     * @param k6Config A loadtest configuration better suited for k6
     * @return String (only for RabbitMQ)
     */
    fun produce(k6Config: K6Configuration): String {
        logger.info("Producing k6 configuration\n" + k6Config.toString())
        val message = messageConverter.toMessage(k6Config, MessageProperties().apply {  })
        template.convertAndSend(
                k6QueueRoutingKey,
            message

        )
        return "K6 LOADTEST CONFIGURATION WAS PRODUCED"
    }
}
