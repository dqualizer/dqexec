package dqualizer.dqexec.util

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.stereotype.Component

/**
 * ProcessLogger writes the console output of a process into a text file If an error occurs, the
 * error message will be written into the console
 */
@Component
class ProcessLogger {
  private val logger = Logger.getGlobal()

  @Throws(IOException::class, InterruptedException::class)
  fun log(
    process: Process,
    logFileBasePath: Path,
    wait: Boolean = true,
  ) {
    // create and connect log files to process
    linkOutputStreamToFile(logFileBasePath, "log", process.inputStream)
    linkOutputStreamToFile(logFileBasePath, "err", process.errorStream)

    logger.info("Writing log to ${logFileBasePath.normalize()}.log")
    logger.info("Writing error log to ${logFileBasePath.normalize()}.err")

    if (wait) {
      waitForProcess(process)
      val exitValue = process.exitValue()
      if (exitValue != 0) logError(process)
    }
  }

  private fun linkOutputStreamToFile(
    logFileBasePath: Path,
    fileExtension: String,
    inputStream: InputStream,
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
