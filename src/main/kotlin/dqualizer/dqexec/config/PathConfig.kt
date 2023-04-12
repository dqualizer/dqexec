package dqualizer.dqexec.config

import org.springframework.context.annotation.Configuration

/**
 * Configuration for local file paths
 */
@Configuration
class PathConfig {
    val constants = "constant/constants.json"
        get() = resources + field
    private val resources = resourcePath

    //remove '/' at the beginning of the string
    private val resourcePath: String
        private get() = this.javaClass.classLoader
            .getResource("")
            .file
            .substring(1) //remove '/' at the beginning of the string
}