package dqualizer.dqexec.input

import dqualizer.dqexec.adapter.resilience.CtkAdaptationService
import dqualizer.dqexec.adapter.loadtest.K6AdaptationService
import dqualizer.dqexec.instrumentation.Monitoring
import io.github.dqualizer.dqlang.types.rqa.configuration.RQAConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload

@Configuration
class RQAConfigConsumer(
  private val monitoring: Monitoring,
  private val k6AdaptationService: K6AdaptationService,
  private val ctkAdaptationService: CtkAdaptationService
) {

  private val log = KotlinLogging.logger {}

  @RabbitListener(queues = ["\${dqualizer.messaging.queues.rqaDefinitionReceiverQueue.name}"])
  fun receive(@Payload rqaConfiguration: RQAConfiguration, @Headers headers: MessageHeaders) {
    log.debug { "${"Received an RQA Configuration: {}"} $rqaConfiguration" }

    try {
      if (rqaConfiguration.loadConfiguration.loadTestArtifacts.isNotEmpty()) {
        log.debug { "Applying load tests" }
        Thread({ applyLoadTest(rqaConfiguration) }, "loadTest-thread").start()
      }

      if (rqaConfiguration.monitoringConfiguration.serviceMonitoringConfigurations.isNotEmpty()) {
        log.debug { "Applying monitoring" }
        Thread({ applyMonitoring(rqaConfiguration) }, "monitoring-thread").start()
      }
      if (rqaConfiguration.resilienceConfiguration.resilienceTestArtifacts.isNotEmpty()) {
        log.debug { "Applying resilience tests" }
        Thread({ applyResilienceTest(rqaConfiguration) }, "resilienceTest-thread").start()
      }
    } catch (e: Exception) {
      log.error { "Applying RQA failed: $e" }
      throw RuntimeException(e)
    }
  }

  private fun applyLoadTest(rqaConfiguration: RQAConfiguration) {
    // TODO Condition to choose load testing tool
    k6AdaptationService.adaptToK6(rqaConfiguration.loadConfiguration)

    //gatlingAdaptationService.adaptToGatling(rqaConfiguration.loadConfiguration)
  }

  private fun applyMonitoring(rqaConfiguration: RQAConfiguration) {
    monitoring.apply(rqaConfiguration.monitoringConfiguration, rqaConfiguration.context)
  }

  private fun applyResilienceTest(rqaConfiguration: RQAConfiguration) {
    ctkAdaptationService.adaptToCtk(rqaConfiguration.resilienceConfiguration)
  }
}
