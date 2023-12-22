package dqualizer.dqexec.adapter

// import dqualizer.dqexec.output.K6ConfigProducer
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.resilienceTestRunner.CtkRunner
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class CtkAdaptationService(
   private val adapter: CtkAdapter,
   private val ctkRunner: CtkRunner
) {
    private val log = Logger.getLogger(this.javaClass.name)

    @RabbitListener(queues = ["\${dqualizer.messaging.queues.resilienceTestConfigurationQueue.name}"])
    private fun receive(@Payload resilienceTestConfiguration: ResilienceTestConfiguration) {
        log.info("Received resilience test configuration\n$resilienceTestConfiguration")
        start(resilienceTestConfiguration)
    }

    private fun start(resilienceTestConfiguration: ResilienceTestConfiguration ) {
        val ctkConfiguration = adapter.adapt(resilienceTestConfiguration)
        log.info("### CONFIGURATION ADAPTED ###")
        log.info("CTK config" + ctkConfiguration)
        ctkRunner.start(ctkConfiguration)

    }
}
