package dqualizer.dqexec.util

import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Helps to read a local file and get the content as a string
 * ( Handle the try-catch-block )
 */
@Service
class SafeFileReader {
    fun readFile(path: Path): String {
        try {
            return Files.readString(path)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}