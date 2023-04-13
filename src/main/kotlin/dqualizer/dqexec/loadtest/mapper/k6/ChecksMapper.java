package dqualizer.dqexec.loadtest.mapper.k6;

import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Checks;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

/**
 * Maps the expected responses to Javascript-Code
 */
@Component
public class ChecksMapper implements k6Mapper {

    @Override
    public String map(Request request) {
        StringBuilder checksBuilder = new StringBuilder();
        Checks checks = request.getChecks();

        int duration = checks.getDuration();
        String durationScript = String.format("\t'Duration < %d': x => x.timings && x.timings.duration < %d,%s",
                duration, duration, newLine);
        checksBuilder.append(durationScript);

        String type = request.getType();
        LinkedHashSet<Integer> statusCodes = checks.getStatusCodes();
        StringBuilder statusBuilder = new StringBuilder();
        for(int status: statusCodes) {
            String statusBooleanScript = String.format("x.status == %d || ", status);
            statusBuilder.append(statusBooleanScript);
        }
        String statusScript = String.format("\t'%s status was expected': x => x.status && (%sfalse),%s",
                type, statusBuilder, newLine);
        checksBuilder.append(statusScript);

        return String.format("check(response, {%s%s});%s",
                newLine, checksBuilder, newLine);
    }
}