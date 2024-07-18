package dqualizer.dqexec.util

import kotlinx.coroutines.*
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
    linkOutputStreamToFile(logFileBasePath, "log", process.inputStream)
    linkOutputStreamToFile(logFileBasePath, "err", process.errorStream)

    logger.info("Writing log to ${logFileBasePath.normalize()}.log")
    logger.info("Writing error log to ${logFileBasePath.normalize()}.err")

    waitForProcess(process)
    val exitValue = process.exitValue()
    if (exitValue != 0) logError(process)
  }

  private fun linkOutputStreamToFile(
    logFileBasePath: Path,
    fileExtension: String,
    inputStream: InputStream
  ) {
    CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
      val targetFile = File("$logFileBasePath.$fileExtension")
      targetFile.parentFile.mkdirs()
      targetFile.delete()
      targetFile.createNewFile()

      val outputStream = FileOutputStream("$logFileBasePath.$fileExtension")
      IOUtils.copy(inputStream, outputStream)
      outputStream.close()
    }
  }

  @Throws(IOException::class)
  private fun logError(process: Process) {
    val errorStream = process.errorStream
    val errorMessage = String(errorStream.readAllBytes(), StandardCharsets.UTF_8)
    logger.warning(errorMessage)
  }

  @Throws(InterruptedException::class)
  private fun waitForProcess(process: Process) {
    logger.info("Waiting for loadtest to finish...")
    println()
    while (process.isAlive) {
      Thread.sleep(1000)
      print(".")
    }
    println(".")
    logger.info("Loadtest finished")
  }
}
