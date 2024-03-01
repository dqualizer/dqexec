package dqualizer.dqexec.adapter

import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestArtifact
import org.springframework.stereotype.Component

@Component
class K6BackMapping {
  companion object {
    fun generateConfig(rqaId: String, domainId: String, artifact: LoadTestArtifact): String {
      return """
      [global_tags]
        rqaId = "$rqaId"
        domainId = "$domainId"
        systemId = "${artifact.artifact.systemId}"
        activityId = "${artifact.artifact.activityId}"
      
      [[inputs.statsd]]
        service_address = ":8125"

        percentiles = [50.0, 90.0, 95.0, 99.0, 99.9, 99.95, 100.0]

        datadog_extensions = true

      [[processors.strings]]
      [[processors.strings.trim_prefix]]
        field_key = "*"
        prefix = "k6_"
        
      [[processors.strings.trim_prefix]]
        measurement = "*"
        prefix = "k6_"

      [[outputs.influxdb_v2]]
        urls = ["${System.getenv("INFLUX_HOST")}"]

        token = "${"\${DOCKER_INFLUXDB_INIT_ADMIN_TOKEN}"}"
        organization = "${"\${DOCKER_INFLUXDB_INIT_ORG}"}"
        bucket = "${"\${DOCKER_INFLUXDB_INIT_BUCKET}"}"
      """
        .trimIndent()
    }
  }
}
