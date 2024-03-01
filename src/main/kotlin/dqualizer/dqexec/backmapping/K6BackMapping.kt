package dqualizer.dqexec.backmapping

import java.io.File
import java.nio.file.Paths

class K6BackMapping(private val backMapping: String) {
  fun writeFile() {
    val path = Paths.get("telegraf", "telegraf.conf")
    val file = File(path.toString())
    file.writeText(backMapping)
  }
}
