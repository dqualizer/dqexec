package dqualizer.dqexec.util

import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
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
        //create log file and parent directories if necessary
        logFile.parentFile.mkdirs()
        logFile.delete()
        logFile.createNewFile()

        val stdout = process.inputStream
        val stdOutFileStream: OutputStream = FileOutputStream(logFile, true)
        IOUtils.copy(stdout, stdOutFileStream)

        val stderr = process.errorStream
        val stderrFileStream: OutputStream = FileOutputStream(logFile, true)
        IOUtils.copy(stderr, stderrFileStream)

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
        println()
        while (process.isAlive) {
            Thread.sleep(1000)
            print(".")
        }
        println(".")
    }
}
