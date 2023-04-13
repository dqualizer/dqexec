package dqualizer.dqexec.loadtest

import org.springframework.stereotype.Component
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Path

/**
 * Writes a k6-script
 */
@Component
class ScriptWriter {
    /**
     * Write a k6-script
     * @param script A list of strings, which should be written inside a file
     * @param scriptPath The path where the file should be created
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(script: List<String?>, scriptPath: Path) {
        val writer = FileWriter(scriptPath.toFile())
        for (line in script) writer.write(line)
        writer.close()
    }
}