package poc.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.Throws
import java.io.IOException
import java.lang.InterruptedException
import java.lang.Process
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException

/**
 * Helps to get the correct hosts for influxDB and the api to which k6 will send requests
 */
@Component
class HostRetriever {
    @Value("\${api.host:127.0.0.1}")
    val aPIHost: String? = null

    @Value("\${dqualizer.influx.host:localhost}")
    val influxHost: String? = null
}