package dqualizer.dqexec.util

import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.stereotype.Component
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.logging.Logger

/**
 * ProcessLogger writes the console output of a process into a text file
 * If an error occurs, the error message will be written into the console
 */
@Component
class ProcessLogger {
    private val logger = Logger.getGlobal()

    @Throws(IOException::class, InterruptedException::class)
    fun log(process: Process, logFileBasePath: Path) {

        //create and connect log files to process
        val stdout = createFileOutputStream(logFileBasePath, "log", process.inputStream)
        val stderr = createFileOutputStream(logFileBasePath, "err", process.errorStream)

        waitForProcess(process)
        stdout.close()
        stderr.close()
        val exitValue = process.exitValue()
        if (exitValue != 0) logError(process)
    }

    private fun createFileOutputStream(
        logFileBasePath: Path,
        fileExtension: String,
        inputStream: InputStream
    ): FileOutputStream {
        val targetFile = File("$logFileBasePath.$fileExtension")
        targetFile.parentFile.mkdirs()
        targetFile.delete()
        targetFile.createNewFile()

        val outputStream = FileOutputStream("$logFileBasePath.$fileExtension")

        IOUtils.copy(inputStream, outputStream)

        return outputStream
    }

    @Throws(IOException::class)
    private fun logError(process: Process) {
        val errorStream = process.errorStream
        val errorMessage = String(errorStream.readAllBytes(), StandardCharsets.UTF_8)
        logger.warning(errorMessage)
    }

    @Throws(InterruptedException::class)
    private fun waitForProcess(process: Process) {
        println()
        while (process.isAlive) {
            Thread.sleep(1000)
            print(".")
        }
        println(".")
    }
}
