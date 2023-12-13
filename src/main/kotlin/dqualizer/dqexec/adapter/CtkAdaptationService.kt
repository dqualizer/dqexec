package dqualizer.dqexec.adapter

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.output.K6ConfigProducer
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkConfiguration
// import dqualizer.dqexec.output.K6ConfigProducer
import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestConfiguration
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class CtkAdaptationService(
   private val adapter: CtkAdapter,
   //private val producer: K6ConfigProducer
) {
    private val log = Logger.getLogger(this.javaClass.name)

    @RabbitListener(queues = ["\${dqualizer.messaging.queues.rqaConfigurationProducerQueue.name}"])
    private fun receive(@Payload ctkConfiguration: CtkConfiguration) {
        log.info("Received loadtest configuration\n$ctkConfiguration")
        start(ctkConfiguration)
    }

    private fun start(ctkConfiguration: CtkConfiguration ) {
        val k6Config = adapter.adapt(ctkConfiguration)
        log.info("### CONFIGURATION ADAPTED ###")
        log.info("CTK config" + k6Config)
        producer.produce(k6Config)
        log.info("### CTK CONFIGURATION PRODUCED ###")
    }
}
