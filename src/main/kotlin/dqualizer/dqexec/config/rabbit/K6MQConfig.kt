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
 * Configuration for the output queue
 */
@Configuration
class K6MQConfig {
    @Bean
    @Qualifier("k6")
    fun k6Exchange(@Value("\${dqualizer.rabbitmq.exchanges.k6:loadtest-k6}") k6Exchange: String?): TopicExchange {
        return TopicExchange(k6Exchange)
    }

    @Bean
    @Qualifier("k6")
    fun k6Queue(@Value("\${dqualizer.rabbitmq.queues.k6:k6}") k6Queue: String?): Queue {
        return Queue(k6Queue, false)
    }

    @Bean
    @Qualifier("k6")
    fun k6POSTBinding(k6Queue: Queue?, k6Exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(k6Queue).to(k6Exchange).with("POST")
    }

    @Bean
    @Qualifier("k6")
    fun k6GETBinding(k6Queue: Queue?, k6Exchange: TopicExchange?): Binding {
        return BindingBuilder.bind(k6Queue).to(k6Exchange).with("GET")
    }
}