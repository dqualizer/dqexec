package dqualizer.dqexec.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Helps to get the correct hosts for influxDB and the api to which k6 will send requests
 */
@Component
class HostRetriever {
    @Value("\${api.host:127.0.0.1}")
    val APIHost: String? = null

    @Value("\${dqualizer.influx.host:localhost}")
    val influxHost: String? = null
}