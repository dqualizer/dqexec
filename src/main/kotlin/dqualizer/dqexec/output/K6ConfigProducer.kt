package dqualizer.dqexec.output

import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestConfiguration
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.logging.Logger

/**
 * Exports the k6 loadtest configuration via RabbitMQ
 */
@Component
class K6ConfigProducer(
  @Value("\${dqualizer.rabbitmq.exchanges.k6}")
  private val exchangeName: String,
  private val template: RabbitTemplate
) {


  private val logger = Logger.getLogger(this.javaClass.name)

  /**
   * Send the k6 configuration to the k6-configuration-runner
   *
   * @param k6Config A loadtest configuration better suited for k6
   * @return String (only for RabbitMQ)
   */
  fun produce(k6Config: LoadTestConfiguration): String {
    logger.info("Producing k6 configuration\n" + k6Config.toString())

    template.convertAndSend(
      exchangeName,
      "POST",
      k6Config
    )
    return "K6 LOADTEST CONFIGURATION WAS PRODUCED"
  }
}
