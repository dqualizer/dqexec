package dqualizer.dqexec.input

import dqualizer.dqexec.adapter.AdaptationManager
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Imports a dqlang loadtest configuration via RabbitMQ
 */
@Component
class LoadTestConfigReceiver {
    @Autowired
    private val manager: AdaptationManager? = null

    /**
     * Import the loadtest configuration and start the adaptation process
     *
     * @param loadTestConfig Imported loadtest configuration
     */
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.exchanges.loadtest:translator-loadtest}"])
    fun receive(@Payload loadTestConfig: LoadTestConfig?) {
        manager!!.start(loadTestConfig!!)
    }
}