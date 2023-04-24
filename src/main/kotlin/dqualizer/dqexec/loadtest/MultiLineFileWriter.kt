package dqualizer.dqexec.loadtest

import org.springframework.stereotype.Component
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Writes a k6-script
 */
@Component
class MultiLineFileWriter {
    /**
     * Write a k6-script
     * @param lines A list of strings, which should be written inside a file
     * @param outputFile The path where the file should be created
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(lines: List<String>, outputFile: File) {
        // Create the file and parent directories
        outputFile.parentFile.mkdirs()
        outputFile.createNewFile()

        val writer = FileWriter(outputFile)
        for (line in lines) writer.write(line)
        writer.close()
    }
}
