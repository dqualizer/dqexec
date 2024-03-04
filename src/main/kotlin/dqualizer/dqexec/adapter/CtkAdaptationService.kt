package dqualizer.dqexec.adapter

import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.resilienceTestRunner.CtkRunner
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import jakarta.annotation.PostConstruct
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class CtkAdaptationService(
        private val adapter: CtkAdapter,
        private val ctkRunner: CtkRunner,
        @Value("\${dqualizer.dqexec.resilienceTesting_enabled:true}")
        private val resilienceTestingEnabled: Boolean
) {
    private val log = Logger.getLogger(this.javaClass.name)

    @RabbitListener(queues = ["\${dqualizer.messaging.queues.resilienceTestConfigurationQueue.name}"])
    private fun receive(@Payload resilienceTestConfiguration: ResilienceTestConfiguration) {

        if (resilienceTestingEnabled){
            log.info("Received resilience test configuration\n$resilienceTestConfiguration")
            start(resilienceTestConfiguration)
        }

        else{
            log.info("Resilience testing was disabled in application.yaml: received resilience test configuration was not processed.")
        }
    }

    private fun start(resilienceTestConfiguration: ResilienceTestConfiguration ) {
        val ctkConfiguration = adapter.adapt(resilienceTestConfiguration)
        log.info("### CONFIGURATION ADAPTED ###")
        log.info("CTK config" + ctkConfiguration)
        ctkRunner.start(ctkConfiguration)

    }
}
