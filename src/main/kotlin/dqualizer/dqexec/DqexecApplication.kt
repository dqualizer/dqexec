package dqualizer.dqexec

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@EnableMongoRepositories
@SpringBootApplication
class DqexecApplication

fun main(args: Array<String>) {
  runApplication<DqexecApplication>(*args)
}
