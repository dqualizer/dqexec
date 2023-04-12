package dqualizer.dqexec.config.rabbit

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for the input queue
 */
@Configuration
class TranslatorMQConfig {
    @Bean
    @Qualifier("loadtest")
    fun loadTestExchange(
        @Value("\${dqualizer.rabbitmq.exchanges.loadtest:translator-loadtest}") loadtestExchange: String?
    ): TopicExchange {
        return TopicExchange(loadtestExchange)
    }

    @Bean
    @Qualifier("loadtest")
    fun loadTestQueue(@Value("\${dqualizer.rabbitmq.queues.loadtest:loadtest") loadtestQueue: String?): Queue {
        return Queue(loadtestQueue, false)
    }

    @Bean
    @Qualifier("loadtest")
    fun loadTestBinding(loadTestQueue: Queue?, loadTestExchange: TopicExchange?): Binding {
        return BindingBuilder.bind(loadTestQueue).to(loadTestExchange).with("GET")
    }
}