package dqualizer.dqexec.config

import org.springframework.context.annotation.Configuration

/**
 * Configuration for local file paths
 */
@Configuration
class PathConfig {

    val script = "poc/scripts/createdScript"
    val logging = "poc/logging/logging"

    val constants = "constant/constants.json"
        get() = resources + field
    val resources = resourcePath

    //remove '/' at the beginning of the string
    final val resourcePath: String
        get() = this.javaClass.classLoader
            .getResource("")
            .file
            .substring(1) //remove '/' at the beginning of the string


    fun getScript(counter: Int): String {
        return resources + script + counter + ".js"
    }

    fun getLogging(counter1: Int, counter2: Int): String {
        return resources + logging + counter1 + "-" + counter2 + ".txt"
    }


}