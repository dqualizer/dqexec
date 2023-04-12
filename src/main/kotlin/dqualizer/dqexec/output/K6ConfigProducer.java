package dqualizer.dqexec.output;

import dqualizer.dqexec.config.rabbit.RabbitConstants;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.K6Config;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Exports the k6 loadtest configuration via RabbitMQ
 */
@Component
@RequiredArgsConstructor
public class K6ConfigProducer {

    private final RabbitTemplate template;

    /**
     * Send the k6 configuration to the k6-configuration-runner
     *
     * @param k6Config A loadtest configuration better suited for k6
     * @return String (only for RabbitMQ)
     */
    public String produce(K6Config k6Config) {
        template.convertAndSend(
                RabbitConstants.K6_EXCHANGE,
                "POST",
                k6Config
        );

        return "K6 LOADTEST CONFIGURATION WAS PRODUCED";
    }
}