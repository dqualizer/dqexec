package dqualizer.dqexec.config.rabbit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Constants for RabbitMQ
 */
@Configuration("dqualizer.rabbit.queues")
public class RabbitConstants {

    @Value("${dqualizer.rabbitmq.queues.loadtest:loadtest")
    public final String LOADTEST_QUEUE = "loadtest";

    @Value("${dqualizer.rabbitmq.queues.k6:k6}")
    public final String K6_QUEUE = "k6";

    @Value("${dqualizer.rabbitmq.exchanges.loadtest:translator-loadtest}")
    public final String LOADTEST_EXCHANGE = "translator-loadtest";

    @Value("${dqualizer.rabbitmq.exchanges.k6:loadtest-k6}")
    public final String K6_EXCHANGE = "loadtest-k6";
}