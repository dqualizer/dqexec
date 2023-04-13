package dqualizer.dqexec.util

import org.springframework.stereotype.Service
import kotlin.Throws
import java.io.IOException
import java.lang.InterruptedException
import java.lang.Process
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Helps to read a local file and get the content as a string
 * ( Handle the try-catch-block )
 */
@Service
class SafeFileReader {
    fun readFile(path: String?): String {
        val text: String
        text = try {
            Files.readString(Paths.get(path))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return text
    }
}