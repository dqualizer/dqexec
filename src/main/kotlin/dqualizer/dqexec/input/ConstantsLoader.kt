package dqualizer.dqexec.input

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.exception.InvalidConstantsSchemaException
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.LoadTestConstants
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Load a local file with load test constants
 */
@Component
class ConstantsLoader(private val paths: PathConfig) {

    fun load(): LoadTestConstants {
        val constantsPath = paths.constants
        var constantsString: String? = ""
        constantsString = try {
            Files.readString(constantsPath)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val objectMapper = ObjectMapper()
        val loadTestConstants: LoadTestConstants
        loadTestConstants = try {
            objectMapper.readValue(constantsString, LoadTestConstants::class.java)
        } catch (e: Exception) {
            throw InvalidConstantsSchemaException(e.message)
        }
        return loadTestConstants
    }
}