package dqualizer.dqexec.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * @author Lion Wagner
 */
@Configuration
data class K6ExecutorConfiguration(
  @Value("\${dqualizer.dqexec.export.k6.influx.token}")
  val influxdbToken: String,
  @Value("\${dqualizer.dqexec.export.k6.influx.bucket}")
  val influxdbBucket: String,
  @Value("\${dqualizer.dqexec.export.k6.influx.organization}")
  val influxdbOrganization: String
)
