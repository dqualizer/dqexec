package dqualizer.dqexec.loadtest.mapper.k6

import io.github.dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the expected responses to Javascript-Code
 */
@Component
class ChecksMapper : K6Mapper {
    override fun map(request: Request): String {
        val checksBuilder = StringBuilder()
        val checks = request.checks
        val duration = checks.duration
        val durationScript = String.format(
            "\t'Duration < %d': x => x.timings && x.timings.duration < %d,%s",
            duration, duration, K6Mapper.newLine
        )
        checksBuilder.append(durationScript)
        val type = request.type
        val statusCodes = checks.statusCodes
        val statusBuilder = StringBuilder()
        for (status in statusCodes) {
            val statusBooleanScript = String.format("x.status == %d || ", status)
            statusBuilder.append(statusBooleanScript)
        }
        val statusScript = String.format(
            "\t'%s status was expected': x => x.status && (%sfalse),%s",
            type, statusBuilder, K6Mapper.newLine
        )
        checksBuilder.append(statusScript)
        return String.format(
            "check(response, {%s%s});%s",
            K6Mapper.newLine, checksBuilder, K6Mapper.newLine
        )
    }
}
