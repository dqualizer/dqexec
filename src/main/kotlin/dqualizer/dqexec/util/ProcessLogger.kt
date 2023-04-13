package poc.util

import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.stereotype.Component
import kotlin.Throws
import java.io.IOException
import java.lang.InterruptedException
import java.lang.Process
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * ProcessLogger writes the console output of a process into a text file
 * If an error occurs, the error message will be written into the console
 */
@Component
class ProcessLogger {
    private val logger = Logger.getGlobal()

    @Throws(IOException::class, InterruptedException::class)
    fun log(process: Process, logFile: File) {
        val inputStream = process.inputStream
        val outputStream: OutputStream = FileOutputStream(logFile)
        IOUtils.copy(inputStream, outputStream)
        waitForProcess(process)
        val exitValue = process.exitValue()
        if (exitValue != 0) logError(process)
    }

    @Throws(IOException::class)
    private fun logError(process: Process) {
        val errorStream = process.errorStream
        val errorMessage = String(errorStream.readAllBytes(), StandardCharsets.UTF_8)
        logger.warning(errorMessage)
    }

    @Throws(InterruptedException::class)
    private fun waitForProcess(process: Process) {
        while (process.isAlive) {
            Thread.sleep(2000)
            println("...")
        }
    }
}