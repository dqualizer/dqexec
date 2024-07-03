package dqualizer.dqexec

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DqexecApplication

fun main(args: Array<String>) {
  runApplication<DqexecApplication>(*args)
}
