package dqualizer.dqexec.adapter

import dqualizer.dqexec.config.StartupConfig
import dqualizer.dqexec.loadtest.ConfigRunner
// import dqualizer.dqexec.output.K6ConfigProducer
import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestConfiguration
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.logging.Logger

/**
 * Manages the whole adaptation process
 * 1. Adapt the imported loadtest configuration to a k6-configuration
 * 2. Export the k6-configuration
 */
@Service
class K6AdaptationService(
   private val adapter: K6Adapter,
   private val k6ConfigRunner: ConfigRunner,
   @Value("\${dqualizer.dqexec.loadTesting_enabled:true}")
   private val loadTestingEnabled: Boolean,
   private val startupConfig: StartupConfig
) {
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Import the loadtest configuration and start the adaptation process
     *
     * @param loadTestConfig Imported loadtest configuration
     */
    //TODO: extract and make generic for different load types
    @RabbitListener(queues = ["\${dqualizer.messaging.queues.loadTestConfigurationQueue.name}"])
    private fun receive(@Payload loadTestConfig: LoadTestConfiguration) {

        if (loadTestingEnabled){
            if (startupConfig.isUserAuthenticated()){
                log.info("Received loadtest configuration\n$loadTestConfig")
                start(loadTestConfig)
            }
            else{
                log.info("Missing dqexec authentication: received load test configuration could not be processed.")
            }
        }
        else{
            log.info("Load testing was disabled in application.yaml: received load test configuration was not processed.")
        }

    }

    private fun start(loadTestConfig: LoadTestConfiguration  ) {
        val k6Config = adapter.adapt(loadTestConfig)
        log.info("### CONFIGURATION ADAPTED ###")
        log.info("k6 config" + k6Config)
        k6ConfigRunner.start(k6Config)
    }
}
