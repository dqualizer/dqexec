package dqualizer.dqexec.config.rabbit

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
    private val monitoring: Monitoring
) {

    private val log = KotlinLogging.logger {}

    @RabbitListener(queues = ["\${dqualizer.messaging.queues.loadtest.name}"])
    fun receive(@Payload rqaConfiguration: RQAConfiguration, @Headers headers: MessageHeaders) {
        log.debug { "${"Received an RQA Configuration: {}"} $rqaConfiguration" }

        try {
            applyMonitoring(rqaConfiguration)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun applyMonitoring(rqaConfiguration: RQAConfiguration) {
        monitoring.apply(rqaConfiguration.monitoringConfiguration, rqaConfiguration.context)
    }
}
