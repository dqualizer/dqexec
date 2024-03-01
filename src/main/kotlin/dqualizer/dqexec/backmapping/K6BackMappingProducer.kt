package dqualizer.dqexec.backmapping

import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.logging.Logger

/** Signals that the load tests are done to RabbitMQ */
@Component
class K6BackMappingProducer(
  val template: RabbitTemplate,
  val messageConverter: MessageConverter,
) {
  @Value("\${dqualizer.messaging.queues.k6.back-mapping}")
  private lateinit var k6BackMappingRoutingKey: String

  private val logger = Logger.getLogger(this.javaClass.name)

  /**
   * Signal that load tests are done
   *
   * @return String (only for RabbitMQ)
   */
  fun produce(rqaId: String): String {
    logger.info("Load tests are done and metrics are collected. Data can now be collected")
    val message = messageConverter.toMessage(rqaId, MessageProperties().apply {})
    template.convertAndSend(
      k6BackMappingRoutingKey,
      message,
    )
    return "K6 BACKMAPPING IS READY TO BE COLLECTED"
  }
}
