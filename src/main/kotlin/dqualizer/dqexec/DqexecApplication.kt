package dqualizer.dqexec

import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

typealias RuntimeQualityAnalysisConfiguration = Request

@SpringBootApplication
class DqexecApplication

fun main(args: Array<String>) {
    runApplication<DqexecApplication>(*args)
}
