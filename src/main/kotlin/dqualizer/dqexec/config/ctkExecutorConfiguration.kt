package dqualizer.dqexec.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class CtkExecutorConfiguration(
    @Value("\${dqualizer.dqexec.export.k6.influx.token}")
    val influxdbToken: String,
    @Value("\${dqualizer.dqexec.export.k6.influx.bucket}")
    val influxdbBucket: String,
    @Value("\${dqualizer.dqexec.export.k6.influx.organization}")
    val influxdbOrganization: String
)
